/**
 * Created by gorshkov on 31.05.2018.
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class BinaryCompare {

    private double maxMismatch = 0;
    private int mismatch = 0;
    private int maxFileSize = 5242880;

    private File firstFile = null;
    private File secondFile = null;

    private boolean compareBigFiles(File one, File two, int goodwillFact, int bufferSize, boolean moreGoodwill, boolean compareSorted) throws IOException {

        BufferedInputStream buffy = new BufferedInputStream(new FileInputStream(one));
        BufferedInputStream buffy2 = new BufferedInputStream(new FileInputStream(two));
        byte[] b1 = new byte[maxFileSize];
        byte[] b2 = new byte[maxFileSize];
        byte[][] parts = null;
        int plus = 0;
        int minus = 0;
        while ((buffy.read(b1) != -1) && (buffy2.read(b2) != -1)) {
            if (compareSorted) {
                Arrays.sort(b1);
                Arrays.sort(b2);
            }
            parts = createParts(b1, bufferSize);
            if (moreGoodwill) {
                if (compareMoreGoodwill(parts, b2, goodwillFact, bufferSize)) {
                    plus++;
                }
                else {
                    minus++;
                }
            }
            else {
                if (compareGoodwill(parts, b2, goodwillFact, bufferSize)) {
                    plus++;
                }
                else {
                    minus++;
                }
            }
        }
        maxMismatch = (plus + minus) * goodwillFact / 100;
        mismatch = minus;
        buffy.close();
        buffy2.close();
        if (minus < maxMismatch) {
            return true;
        }
        return false;
    }

    private ArrayList<File> listDir(File dir) {

        File[] files = dir.listFiles();
        ArrayList<File> f = new ArrayList<File>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    f.addAll(listDir(files[i]));
                }
                else {
                    f.add(files[i]);
                }
            }
        }
        return f;
    }

    public ArrayList<File[]> compareFilesInDirectory (File dir, int goodwillFact, boolean moreGoodwill, boolean compareSorted) throws IOException {

        ArrayList<File[]> matches = new ArrayList<File[]>();
        ArrayList<File> files = listDir(dir);
        int size = files.size();
        int bufferSize = 0;
        byte[][] parts = null;
        byte[] buff = null;

        Collections.sort(files, new Comparator<File>() {
            public int compare(File one, File two) {
                if (one.length() > two.length()) {
                    return 1;
                }
                return -1;
            }
        });

        for (int i = 0; i < size; i++) {
            System.out.println(i);
            setFirstFile(files.get(i));
            bufferSize = getMiddleBuffer(getFirstFile());
            if (getFirstFile().length() < getMaxFileSize()) {
                buff = readFile(getFirstFile());
                if (compareSorted) {
                    Arrays.sort(buff);
                }
                parts = createParts(buff, bufferSize);
            }
            for (int j = i + 1; j < size; j++) {
                setSecondFile(files.get(j));
                if (getSecondFile().length() < getMaxFileSize()) {
                    if ((getSecondFile().length() - getFirstFile().length()) > getFirstFile().length() * goodwillFact / 100D) {
                        break;
                    }
                    buff = readFile(getSecondFile());
                    if (compareSorted) {
                        Arrays.sort(buff);
                    }
                    if (moreGoodwill) {
                        if (compareMoreGoodwill(parts, buff, goodwillFact, bufferSize)) {
                            matches.add(new File[] {getFirstFile(), getSecondFile()});
                        }
                    }
                    else {
                        if (compareGoodwill(parts, buff, goodwillFact, bufferSize)) {
                            matches.add(new File[] {getFirstFile(), getSecondFile()});
                        }
                    }
                }
                else if (compareGoodwill(getFirstFile(), getSecondFile(), goodwillFact, bufferSize, false, false)) {
                    matches.add(new File[] {files.get(i), files.get(j)});
                }
            }
        }
        return matches;
    }

    public boolean compareExactly(File one, File two) throws IOException {

        if (one.length()==two.length()) {
            FileInputStream fis1 = new FileInputStream(one);
            FileInputStream fis2 =new FileInputStream(two);
            int temp = 0;
            while ((temp = fis1.read()) != -1) {
                if (temp != fis2.read()) {
                    fis1.close();
                    fis2.close();
                    return false;
                }
            }
            fis1.close();
            fis2.close();
            return true;
        }
        return false;
    }

    public boolean compareGoodwill(File one, File two, int goodwillFact, int bufferSize, boolean moreGoodwill, boolean compareSorted) throws IOException {

        if (one.length() < two.length()) {
            String temp = two.getAbsolutePath();
            two = new File(one.getAbsolutePath());
            one = new File(temp);
        }
        long diff = one.length() - two.length();
        if (diff > one.length() * goodwillFact / 100D) {
            return false;
        }
        if (one.length() > maxFileSize) {
            return compareBigFiles(one, two, goodwillFact, bufferSize, moreGoodwill, compareSorted);
        }
        byte[] file = null;
        byte[][] parts = null;
        if (compareSorted) {
            file = readFile(one);
            Arrays.sort(file);
            parts = createParts(file, bufferSize);
            file = readFile(two);
            Arrays.sort(file);
        }
        else {
            parts = createParts(readFile(one), bufferSize);
            file = readFile(two);
        }
        if (moreGoodwill) {
            return compareMoreGoodwill(parts, file, goodwillFact, bufferSize);
        }
        return compareGoodwill(parts, file, goodwillFact, bufferSize);
    }

    public boolean compareGoodwill(byte[][] parts, byte[] file, int goodwillFact, int bufferSize) {

        maxMismatch = parts.length * goodwillFact / 100D;
        mismatch = 0;
        int pos = 0;
        int temp = 0;
        int lastBuf = (int)maxMismatch * bufferSize;
        for (int i = 0; i < parts.length; i++) {
            temp = myIndexOf(file, parts[i], pos, (int)(pos + lastBuf));
            if (temp != -1) {
                pos = temp + bufferSize;
            }
            else {
                mismatch++;
                lastBuf = (int)(maxMismatch - mismatch) * bufferSize;
                if (mismatch > maxMismatch) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean compareMoreGoodwill(byte[][] parts, byte[] file, int goodwillFact, int bufferSize) {

        maxMismatch = parts.length * goodwillFact / 50D;
        mismatch = 0;
        int pos = 0;
        int temp = 0;
        int lastBuf = (int)maxMismatch / 2 * bufferSize;
        int[] notFound = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            temp = myIndexOf(file, parts[i], pos, (int)(pos + lastBuf));
            if (temp != -1) {
                replaceRange(file, temp, temp + bufferSize, Byte.MIN_VALUE);
                pos = temp;
            }
            else {
                notFound[mismatch] = i;
                mismatch++;
                lastBuf = (int)(maxMismatch / 2 - mismatch) * bufferSize;
                if (mismatch > maxMismatch) {
                    return false;
                }
            }
        }
        for (int i = mismatch - 1; i > -1; i--) {
            temp = myIndexOf(file, parts[notFound[i]], 0, file.length);
            if (temp != -1) {
                replaceRange(file, temp, temp + bufferSize, (byte)-1);
            }
            else {
                mismatch++;
                if (mismatch > maxMismatch) {
                    return false;
                }
            }
        }
        return true;
    }

    public int[] getOptimizedBufferRange(File file) {

        int[] range = new int[2];
        range[0] = (int)(file.length() * 0.04D / 100D);
        range[1] = (int)(file.length() * 0.14D / 100D);
        range[0] = range[0] < 5 ? 5 : range[0];
        range[1] = range[1] < 10 ? 10 : range[1];
        return range;
    }

    public int getMiddleBuffer(File file) {

        int i = (int)(file.length() * 0.07D / 100D);
        return i < 7 ? 7 : i;
    }

    public byte[][] createParts(byte[] data, int buffer) {

        byte[][] parts = null;
        if (data.length % buffer == 0) {
            parts = new byte[data.length / buffer][buffer];
        }
        else {
            parts = new byte[data.length / buffer + 1][buffer];
        }
        for (int i = 0, k = 0; i < parts.length; i++) {
            for (int j = 0; j < parts[i].length; j++, k++) {
                if (k >= data.length) {
                    parts[i][j] = -1;
                }
                else {
                    parts[i][j] = data[k];
                }
            }
        }
        return parts;
    }

    public byte[] readFile(File file) throws IOException, OutOfMemoryError {

        FileChannel fc = new FileInputStream(file).getChannel();
        ByteBuffer buff = ByteBuffer.allocate((int)file.length());
        fc.read(buff);
        fc.close();
        return buff.array();
    }

    public int myIndexOf(byte[] file, byte[] target, int startPos, int endPos) {

        int tlength = target.length;
        int startInt = target[0];
        if (endPos > file.length) {
            endPos = file.length;
        }
        if (target.length > endPos - startPos) {
            return -1;
        }
        for (int i = 0, count = 0, pos = startPos - 1;pos + tlength < endPos;) {
            while(++pos + tlength < endPos && file[pos] != startInt);
            i = pos;
            count = 0;
            for (;count < tlength && file[i] == target[count];i++, count++);
            if (count == tlength) {
                return pos;
            }
        }
        return -1;
    }

    public void replaceRange(byte[] array, int start, int end, byte number) {

        for (int i = start; i < end; i++) {
            array[i] = number;
        }
    }

    public double getMaxMismatch() {
        return maxMismatch;
    }

    public void setMaxMismatch(double maxMismatch) {
        this.maxMismatch = maxMismatch;
    }

    public int getMismatch() {
        return mismatch;
    }

    public void setMismatch(int mismatch) {
        this.mismatch = mismatch;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public File getFirstFile() {
        return firstFile;
    }

    public void setFirstFile(File firstFile) {
        this.firstFile = firstFile;
    }

    public File getSecondFile() {
        return secondFile;
    }

    public void setSecondFile(File secondFile) {
        this.secondFile = secondFile;
    }

    public static void main(String[] args) throws IOException {

/*        final BinaryCompare binaryCompare = new BinaryCompare();
        final File one = new File("D:\\test\\Новая папка - копия\\m - копия (2).txt");
        final File two = new File("D:\\test\\Новая папка - копия\\m - копия.txt");
        System.out.println(binaryCompare.compareExactly(one, two));*/

        BinaryCompare fc = new BinaryCompare();
        long start = System.currentTimeMillis();
        ArrayList<File[]> matches = fc.compareFilesInDirectory(
                new File("Z:\\"), 25, true, false);
        System.out.println("Time needed: " + (System.currentTimeMillis() - start));
        System.out.println("Matches:");
        for (File[] f : matches) {
            System.out.println(f[0].getAbsolutePath() + " *matches* " + f[1].getAbsolutePath());
        }
    }
}
