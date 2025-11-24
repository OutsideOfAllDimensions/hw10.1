import java.io.*;
import java.util.*;

public class ClassicIOCacheWithLimit {
    private Map<String, FileCacheEntry> cache;
    private int maxSize;

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
        if (cache.size() >= maxSize) {
            removeOldestEntry();
        }

        String content = readFileContent(file);
        long now = System.currentTimeMillis();

        FileCacheEntry entry = new FileCacheEntry(content, now, currentModifiedTime);
        cache.put(absolutePath, entry);

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

        try (BufferedReader reader = new BufferedReader(new FileReader(file), 8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return content.toString();
    }

    public void invalidate(String filePath) {
        File file = new File(filePath);
        cache.remove(file.getAbsolutePath());
    }

    public void invalidateAll() {
        cache.clear();
    }

    public boolean isCached(String filePath) {
        File file = new File(filePath);
        return cache.containsKey(file.getAbsolutePath());
    }

    public int getCachedFilesCount() {
        return cache.size();
    }

    public long getCacheSizeInMemory() {
        long size = 0;
        for (FileCacheEntry entry : cache.values()) {
            size += entry.content.length() * 2;
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
            long fileSize = entry.getValue().content.length() * 2;
            String lastRead = new Date(entry.getValue().lastReadTime).toString();
            System.out.println("  " + fileName + " - " + fileSize + " байт, прочитан: " + lastRead);
        }
    }

    public static void main(String[] args) throws Exception {
        ClassicIOCacheWithLimit cache = new ClassicIOCacheWithLimit(2);

        System.out.println(cache.readFile("1.txt"));
        System.out.println(cache.readFile("2.txt"));

        cache.printCacheStats();

        System.out.println(cache.readFile("3.txt")); 
        cache.printCacheStats();

        cache.invalidate("2.txt");
        cache.printCacheStats();

        cache.invalidateAll();
        cache.printCacheStats();
    }
}