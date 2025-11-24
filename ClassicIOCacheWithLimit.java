import java.io.*;
import java.util.*;

public class ClassicIOCacheWithLimit {
    private Map<String, FileCacheEntry> cache;
    private int maxSize;

    public ClassicIOCacheWithLimit(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new HashMap<>();
    }

    public ClassicIOCacheWithLimit() {
        this(100);
    }

    public String readFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Файл не существует: " + filePath);
        }

        String absolutePath = file.getAbsolutePath();
        long currentModifiedTime = file.lastModified();

        if (cache.containsKey(absolutePath)) {
            FileCacheEntry cachedEntry = cache.get(absolutePath);
            if (isCacheValid(cachedEntry, currentModifiedTime)) {
                cachedEntry.lastReadTime = System.currentTimeMillis();
                return cachedEntry.content;
            }
        }

        return updateCache(file, absolutePath, currentModifiedTime);
    }

    private boolean isCacheValid(FileCacheEntry cachedEntry, long currentModifiedTime) {
        return cachedEntry.lastModifiedTimeAtRead == currentModifiedTime;
    }

    private String updateCache(File file, String absolutePath, long currentModifiedTime) throws IOException {
        String content = readFileContent(file);
        
        if (cache.size() >= maxSize) {
            removeOldestEntry();
        }

        FileCacheEntry newEntry = new FileCacheEntry(content, System.currentTimeMillis(), currentModifiedTime);
        cache.put(absolutePath, newEntry);
        
        return content;
    }

    private void removeOldestEntry() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, FileCacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().lastReadTime < oldestTime) {
                oldestTime = entry.getValue().lastReadTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileReader fileReader = new FileReader(file);
             BufferedReader reader = new BufferedReader(fileReader, 8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public void invalidate(String filePath) {
        File file = new File(filePath);
        String absolutePath = file.getAbsolutePath();
        cache.remove(absolutePath);
    }

    public void invalidateAll() {
        cache.clear();
    }

    public boolean isCached(String filePath) {
        File file = new File(filePath);
        String absolutePath = file.getAbsolutePath();
        return cache.containsKey(absolutePath);
    }

    public int getCachedFilesCount() {
        return cache.size();
    }

    public long getCacheSizeInMemory() {
        long size = 0;
        for (FileCacheEntry entry : cache.values()) {
            size += entry.content.length() * 2L;
        }
        return size;
    }

    public void printCacheStats() {
        System.out.println("Статистика кэша:");
        System.out.println("Количество файлов: " + getCachedFilesCount());
        System.out.println("Размер в памяти: " + getCacheSizeInMemory() + " байт");
        System.out.println("Максимальный размер: " + maxSize + " файлов");
        System.out.println("Закэшированные файлы:");
        
        for (Map.Entry<String, FileCacheEntry> entry : cache.entrySet()) {
            String fileName = new File(entry.getKey()).getName();
            long fileSize = entry.getValue().content.length() * 2L;
            String lastRead = new Date(entry.getValue().lastReadTime).toString();
            System.out.println("  " + fileName + " - " + fileSize + " байт, прочитан: " + lastRead);
        }
    }

    private static class FileCacheEntry {
        String content;
        long lastReadTime;
        long lastModifiedTimeAtRead;

        FileCacheEntry(String content, long lastReadTime, long lastModifiedTimeAtRead) {
            this.content = content;
            this.lastReadTime = lastReadTime;
            this.lastModifiedTimeAtRead = lastModifiedTimeAtRead;
        }
    }

    public static void main(String[] args) {
        try {
            ClassicIOCacheWithLimit cache = new ClassicIOCacheWithLimit(3);
            
            File testFile1 = new File("test1.txt");
            File testFile2 = new File("test2.txt");
            File testFile3 = new File("test3.txt");
            File testFile4 = new File("test4.txt");
            
            try (PrintWriter writer = new PrintWriter(testFile1)) {
                writer.println("Содержимое файла 1");
            }
            try (PrintWriter writer = new PrintWriter(testFile2)) {
                writer.println("Содержимое файла 2");
            }
            try (PrintWriter writer = new PrintWriter(testFile3)) {
                writer.println("Содержимое файла 3");
            }
            try (PrintWriter writer = new PrintWriter(testFile4)) {
                writer.println("Содержимое файла 4");
            }
            
            System.out.println("Первое чтение файлов:");
            cache.readFile("test1.txt");
            cache.readFile("test2.txt");
            cache.readFile("test3.txt");
            cache.printCacheStats();
            
            System.out.println("\nЧтение файла 4 (должен вытеснить самый старый):");
            cache.readFile("test4.txt");
            cache.printCacheStats();
            
            System.out.println("\nПовторное чтение файла 2 (из кэша):");
            cache.readFile("test2.txt");
            cache.printCacheStats();
            
            System.out.println("\nОчистка кэша:");
            cache.invalidate("test2.txt");
            cache.printCacheStats();
            
            testFile1.delete();
            testFile2.delete();
            testFile3.delete();
            testFile4.delete();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
