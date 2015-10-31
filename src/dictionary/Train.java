/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dictionary;

/**
 *
 * @author Makan
 */
public class Train {

    Wagon first, last, head;
    private int wagonSize, wagons, trainSize;
    public int size;

    public enum Type {

        BYTE,
        INTEGER,
        CHAR,
        DOUBLE
    }
    Type t;

    public Train(int wagonSize, Type t) {
        this.t = t;
        if (wagonSize < 1) {
            wagonSize = 1;
        }
        if (t.equals(Type.BYTE)) {
//            System.out.println("byte recognized.");
            first = new WagonBYTE(wagonSize, 1);
        } else if (t.equals(Type.DOUBLE)) {
            first = new WagonDOUBLE(wagonSize, 1);
        } else if (t.equals(Type.CHAR)) {
            first = new WagonCHAR(wagonSize, 1);
        } else if (t.equals(Type.INTEGER)) {
            first = new WagonINT(wagonSize, 1);
        }
        this.wagonSize = wagonSize;
        first.setPrev(null);
        first.setNext(last);
        head = first;
        last = first;
        wagons = 1;
        size = 0;
        trainSize = wagonSize;
    }

    public void headToStart() {
        head = first;
    }

    public static void main(String args[]) {
        Train array = new Train(5, Type.BYTE);
        for (int b = 0; b < 100; b++) {
            array.setByte(b, (byte) b);
        }
        byte testByte = 12;
        // to test the use of headToStart(), and headToLast(),
        //uncomment their line of execution and pay attention to time of execution.
        long time = System.currentTimeMillis();
        for (int i = 0; i < 100000000; i++) {
//            array.headToEnd();
            array.setByte(97, testByte);
//            array.headToStart();
            array.getByte(0);
        }
        time = System.currentTimeMillis() - time;
        System.out.println("time: " + time + " milliseconds");
//        System.out.println(array.getByte(11));
    }

    public void setDouble(int i, double val) {
        if (i >= size) {
            size = i + 1;
        }
        while (i >= trainSize) {
            addWagon();
        }

        while ((i / wagonSize + 1) > head.getNumber()) {
            head = head.getNext();
        }

        while ((i / wagonSize + 1) < head.getNumber()) {
            head = head.getPrev();
        }
        // at this line, head is pointing to the right wagon.
        head.setDOUBLE(val, (i % wagonSize));
    }

    public double getDouble(int i) {
        while (i >= trainSize) {
            addWagon();
        }
        while ((i / wagonSize + 1) > head.getNumber()) {
            head = head.getNext();
        }

        while ((i / wagonSize + 1) < head.getNumber()) {
            head = head.getPrev();
        }
        return head.getDOUBLE(i % wagonSize);
    }

    public void setChar(int i, char val) {
        if (i >= size) {
            size = i + 1;
        }
        while (i >= trainSize) {
            addWagon();
        }

        while ((i / wagonSize + 1) > head.getNumber()) {
            head = head.getNext();
        }

        while ((i / wagonSize + 1) < head.getNumber()) {
            head = head.getPrev();
        }
        // at this line, head is pointing to the right wagon.
        head.setCHAR(val, (i % wagonSize));
    }

    public char getChar(int i) {
        while (i >= trainSize) {
            addWagon();
        }
        while ((i / wagonSize + 1) > head.getNumber()) {
            head = head.getNext();
        }

        while ((i / wagonSize + 1) < head.getNumber()) {
            head = head.getPrev();
        }
        return head.getCHAR(i % wagonSize);
    }

    public int getInt(int i) {
        while (i >= trainSize) {
            addWagon();
        }
        while ((i / wagonSize + 1) > head.getNumber()) {
            head = head.getNext();
        }

        while ((i / wagonSize + 1) < head.getNumber()) {
            head = head.getPrev();
        }
        return head.getINT(i % wagonSize);
    }

    public void setInt(int i, int val) {
        if (i >= size) {
            size = i + 1;
        }
        while (i >= trainSize) {
            addWagon();
        }
//
        while ((i / wagonSize + 1) < head.getNumber()) {
            head = head.getPrev();
        }
        while ((i / wagonSize + 1) > head.getNumber()) {
            head = head.getNext();
        }
        head.setINT(val, (i % wagonSize));
    }

    public byte getByte(int i) {
        while (i >= trainSize) {
            addWagon();
        }
        while ((i / wagonSize + 1) > head.getNumber()) {
            head = head.getNext();
        }

        while ((i / wagonSize + 1) < head.getNumber()) {
            head = head.getPrev();
        }
        return head.getBYTE(i % wagonSize);
    }

    public void setByte(int i, byte val) {
        if (i >= size) {
            size = i + 1;
        }
        while (i >= trainSize) {
            addWagon();
        }
        while ((i / wagonSize + 1) < head.getNumber()) {
            head = head.getPrev();
        }
        while ((i / wagonSize + 1) > head.getNumber()) {
            head = head.getNext();
        }
        // at this line, head is pointing to the right wagon.
        head.setBYTE(val, (i % wagonSize));
    }

    public int getLength() {
        return size;
    }

    public void clear() {
        size = 0;
        head = first;
    }

    private void addWagon() {
        wagons++;
        trainSize += wagonSize;
        if (t.equals(Type.INTEGER)) {
            last.setNext(new WagonINT(wagonSize, wagons));
        } else if (t.equals(Type.BYTE)) {
            last.setNext(new WagonBYTE(wagonSize, wagons));
        } else if (t.equals(Type.CHAR)) {
            last.setNext(new WagonCHAR(wagonSize, wagons));
        } else if (t.equals(Type.DOUBLE)) {
            last.setNext(new WagonDOUBLE(wagonSize, wagons));
        }
        last.getNext().setPrev(last);
        last = last.getNext();
    }

    public void cutOff(int i) {
        while (1 + (i / wagonSize) < wagons) {
            wagons--;
            last = last.getPrev();
        }
        last.setNext(null);
        if (size >= last.getNumber() * wagonSize) {
            size = last.getNumber() * wagonSize - 1;
        }
        head = first;
    }
}

class WagonDOUBLE extends Wagon {

    private double d[];

    public WagonDOUBLE(int size, int number) {
        super(number);
        d = new double[size];
    }

    @Override
    public void setDOUBLE(double val, int index) {
        d[index] = val;
    }

    @Override
    public double getDOUBLE(int index) {
        return d[index];
    }
}

class WagonCHAR extends Wagon {

    private char c[];

    public WagonCHAR(int size, int number) {
        super(number);
        c = new char[size];
    }

    @Override
    public void setCHAR(char val, int index) {
        c[index] = val;
    }

    @Override
    public char getCHAR(int index) {
        return c[index];
    }
}

class WagonBYTE extends Wagon {

    private byte b[];

    public WagonBYTE(int size, int number) {
        super(number);
        b = new byte[size];
    }

    @Override
    public void setBYTE(byte val, int index) {
        b[index] = val;
    }

    @Override
    public byte getBYTE(int index) {
        return b[index];
    }
}

class WagonINT extends Wagon {

    private int a[];

    public WagonINT(int size, int number) {
        super(number);
        a = new int[size];
    }

    @Override
    public void setINT(int val, int index) {
        a[index] = val;
    }

    @Override
    public int getINT(int index) {
        return a[index];
    }
}

class Wagon {

    public void setDOUBLE(double val, int index) {
    }

    public double getDOUBLE(int index) {
        return -1;
    }

    public void setCHAR(char val, int index) {
    }

    public char getCHAR(int index) {
        return (char) 0;
    }

    public void setINT(int val, int index) {
    }

    public int getINT(int index) {
        return -1;
    }

    public void setBYTE(byte val, int index) {
    }

    public byte getBYTE(int index) {
        return (byte) 1;
    }
    private int number;
    private Wagon next, prev;

    public Wagon(int number) {
        this.number = number;
//        b = new byte[size];
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public Wagon getPrev() {
        return prev;
    }

    public void setPrev(Wagon prev) {
        this.prev = prev;
    }

    public void setNext(Wagon next) {
        this.next = next;
    }

    public Wagon getNext() {
        return next;
    }
}
