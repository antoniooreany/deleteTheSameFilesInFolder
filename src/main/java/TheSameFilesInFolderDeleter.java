import java.io.File;
import java.io.IOException;

/**
 * Created by gorshkov on 30.05.2018.
 */
public class TheSameFilesInFolderDeleter {
    public static void main(String[] args) throws IOException {
        deleteTheSameFilesInFolder(new File("D:\\test"));
    }

    private static void deleteTheSameFilesInFolder(File folder) throws IOException {
        File[] folderEntries = folder.listFiles();
        for (File entry0 : folderEntries) {
            if (entry0.isDirectory()) {
                deleteTheSameFilesInFolder(entry0);
                continue;
            }
            int equalFilesCount = 0;
            for (File entry1 : folderEntries) {
                final long length0 = entry0.length();
                final long length1 = entry1.length();
                if (length0 == length1) {
                    if (new BinaryCompare().compareExactly(entry0, entry1)) { //check for identity byte-by-byte
                        equalFilesCount++;
                    }
                }
                if (equalFilesCount > 1) {
//                    final String entry1Name = entry1.getName();
//                    final String entry1Parent = entry1.getParent();
//                    final File entry1Dest = new File(entry1Parent + "\\____" + entry1Name);
//                    final boolean renameTo = entry1.renameTo(entry1Dest);
                    final boolean deleted = entry0.delete();
                    equalFilesCount = 0;
                }
            }
        }
    }
}



