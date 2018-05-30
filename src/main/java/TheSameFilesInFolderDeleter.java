import java.io.File;

/**
 * Created by gorshkov on 30.05.2018.
 */
public class TheSameFilesInFolderDeleter {
    public static void main(String[] args) {
        deleteTheSameFilesInFolder(new File("D:\\test"));
    }

    private static void deleteTheSameFilesInFolder(File folder) {
        File[] folderEntries = folder.listFiles();
        for (File entry0 : folderEntries) {
            if (entry0.isDirectory()) {
                deleteTheSameFilesInFolder(entry0);
                continue;
            }
            int equalLengthCount = 0;
            for (File entry1 : folderEntries) {
//                System.out.printf("entry0.length() = %d%n", length0);
//                System.out.printf("entry1.length() = %d%n", length1);
                final long length0 = entry0.length();
                final long length1 = entry1.length();
                if (length0 == length1) {
                    equalLengthCount++;
                }
                if (equalLengthCount > 1) {
                    final boolean deleted = entry1.delete();
                    equalLengthCount = 0;
                }
            }
        }
    }
}


