package gputils;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import sun.java2d.pipe.SpanShapeRenderer;

/**
 * Mostly from: https://www.geeksforgeeks.org/min-heap-in-java/
 *
 * Edited by Jordan MacLachlan for use in UCARP 26 Nov 2019
 */
public class MinHeap {
    private static Arc rootArc = new Arc(0, 0, 0, 0, 0, null, 0, 0);
    private Arc[] Heap;
    private int size;
    private int maxsize;
    private TieBreaker tb;

    private static final int FRONT = 1;

    public MinHeap(int maxsize) {
        this.maxsize = maxsize;
        this.size = 0;
        Heap = new Arc[this.maxsize+1];
        Heap[0] = rootArc;
        Heap[0].setPriority(Double.NEGATIVE_INFINITY);
    }

    public int getSize(){
        return size;
    }

    // Function to return the position of
    // the parent for the node currently
    // at pos
    private int parent(int pos) {
        return pos / 2;
    }

    // Function to return the position of the
    // left child for the node currently at pos
    private int leftChild(int pos) {
        return (2 * pos);
    }

    // Function to return the position of
    // the right child for the node currently
    // at pos
    private int rightChild(int pos) {
        return (2 * pos) + 1;
    }

    // Function that returns true if the passed
    // node is a leaf node
    private boolean isLeaf(int pos) {
        if (pos >= (size / 2) && pos <= size)
            return true;
        return false;
    }

    // Function to swap two nodes of the heap
    private void swap(int fpos, int spos) {
        Arc tmp;
        tmp = Heap[fpos];
        Heap[fpos] = Heap[spos];
        Heap[spos] = tmp;
    }

    // Function to heapify the node at pos
    private void minHeapify(int pos) {
        // If the node is a non-leaf node and greater
        // than any of its child
        if (!isLeaf(pos)) {
            if(Double.compare(Heap[leftChild(pos)].getPriority(), Heap[pos].getPriority()) < 0
                || Double.compare(Heap[rightChild(pos)].getPriority(), Heap[pos].getPriority()) < 0) {
                // Swap with the left child and heapify
                // the left child
                if(Double.compare(Heap[leftChild(pos)].getPriority(), Heap[rightChild(pos)].getPriority()) < 1){
                    swap(pos, leftChild(pos));
                    minHeapify(leftChild(pos));
                }
                // Swap with the right child and heapify
                // the right child
                else {
                    swap(pos, rightChild(pos));
                    minHeapify(rightChild(pos));
                }
            }
        }
    }

    // Function to insert a node into the heap
    public void insert(Arc element, TieBreaker tb) {
//        System.out.println("inserting: " + element.getPriority());
        this.tb = tb;
        int index;
        if (size >= maxsize) {
            index = getWorstIndex(element, tb);
            if(index >= 0){
                Heap[index] = null;
            } else return;
        } else index = ++size;

        Heap[index] = element;
        int current = size;

        while(Double.compare(Heap[current].getPriority(), Heap[parent(current)].getPriority()) < 0){
            swap(current, parent(current));
            current = parent(current);
        }
    }

    public int getWorstIndex(Arc element, TieBreaker tb){
        double elementPriority = element.getPriority();
        int res = -1;
        Arc resArc = null;
        for(int i = 0; i < Heap.length; i++){
            Arc arc = Heap[i];
            if(arc != null && (Double.compare(arc.getPriority(), elementPriority) > 0 ||
                    Double.compare(arc.getPriority(), elementPriority) == 0 && tb.breakTie(arc, element) < 0)){
                if(resArc == null || Double.compare(arc.getPriority(), resArc.getPriority()) > 0) {
                    resArc = arc; res = i;
                }
            }
        }
        return res;
    }

    // Function to build the min heap using
    // the minHeapify
    public void minHeap() {
        for (int pos = (size / 2); pos >= 1; pos--) {
            minHeapify(pos);
        }
    }

    public Arc[] getHeap(){
        return Heap;
    }

    // Function to remove and return the minimum
    // element from the heap
    public Arc remove() {
        Arc popped = Heap[FRONT];
        Heap[FRONT] = Heap[size--];
        minHeapify(FRONT);
        return popped;
    }

    public Arc getLowest(){
        Arc lowest = Heap[FRONT];
        return lowest;
    }

    // Function to print the contents of the heap
    public void print() {
        for (int i = 1; i <= size / 2; i++) {
            System.out.print(" PARENT : " + Heap[i]
                    + " LEFT CHILD : " + Heap[2 * i]
                    + " RIGHT CHILD :" + Heap[2 * i + 1]);
            System.out.println();
        }
    }

    // Driver code
//    public static void main(String[] arg)
//    {
//        System.out.println("The Min Heap is ");
//        MinHeap minHeap = new MinHeap(5);
//        minHeap.tb = new SimpleTieBreaker();
//        minHeap.insert(new Arc(70), minHeap.tb);
//        minHeap.insert(new Arc(60), minHeap.tb);
//        minHeap.insert(new Arc(80), minHeap.tb);
//        minHeap.insert(new Arc(30), minHeap.tb);
//        minHeap.insert(new Arc(12), minHeap.tb);
//        minHeap.insert(new Arc(24), minHeap.tb);
//        minHeap.insert(new Arc(120), minHeap.tb);
//        minHeap.insert(new Arc(75), minHeap.tb);
//        minHeap.insert(new Arc(30), minHeap.tb);
//        minHeap.insert(new Arc(40), minHeap.tb);
//        minHeap.insert(new Arc(95), minHeap.tb);
//        minHeap.insert(new Arc(55), minHeap.tb);
//        minHeap.insert(new Arc(120), minHeap.tb);
//        minHeap.insert(new Arc(90), minHeap.tb);
//        minHeap.minHeap();
//
//        minHeap.print();
//        System.out.println("The Min val is " + minHeap.getLowest());
//        minHeap.print();
//
//        for(int i = 0; i < minHeap.Heap.length; i++)
//            System.out.println("val " + i + ": " + minHeap.Heap[i].getPriority());
//    }

    public static void main(String[] arg)
    {
        System.out.println("The Min Heap is ");
        MinHeap minHeap = new MinHeap(5);
        minHeap.tb = new SimpleTieBreaker();
        minHeap.insert(new Arc(0.02722323049001815), minHeap.tb);
        minHeap.insert(new Arc(0.034482758620689655), minHeap.tb);
        minHeap.insert(new Arc(80), minHeap.tb);
        minHeap.insert(new Arc(30), minHeap.tb);
        minHeap.insert(new Arc(12), minHeap.tb);
        minHeap.insert(new Arc(0.03694581280788178), minHeap.tb);
        minHeap.insert(new Arc(120), minHeap.tb);
        minHeap.insert(new Arc(75), minHeap.tb);
        minHeap.insert(new Arc(0.04155124653739612), minHeap.tb);
        minHeap.insert(new Arc(40), minHeap.tb);
        minHeap.insert(new Arc(95), minHeap.tb);
        minHeap.insert(new Arc(55), minHeap.tb);
        minHeap.insert(new Arc(0.02722323049001815), minHeap.tb);
        minHeap.insert(new Arc(90), minHeap.tb);
        minHeap.minHeap();

        minHeap.print();
        System.out.println("The Min val is " + minHeap.getLowest());
        minHeap.print();

        for(int i = 0; i < minHeap.Heap.length; i++)
            System.out.println("val " + i + ": " + minHeap.Heap[i].getPriority());
    }
}
