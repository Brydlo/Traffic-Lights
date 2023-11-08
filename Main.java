package traffic;


import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws IOException {
    Scanner s = new Scanner(System.in);
    ClearScreen clear = new ClearScreen();
    String error = "Incorrect option";
    String inputError = "Error! Incorrect input. Try again: ";
    int numberOfRoads;
    int inter;
    int numOfInputRoads = 0;

    System.out.println("Welcome to the traffic management system!");

    System.out.print("Input the number of roads: ");
    do {
      try {
        numberOfRoads = Integer.parseInt(s.nextLine());
        if (numberOfRoads >= 1) break;
        else System.out.print(inputError);
      } catch (IllegalArgumentException e) {
        System.out.print(inputError);
      }
    } while (true);

    System.out.print("Input the interval: ");
    do {
      try {
        inter = Integer.parseInt(s.nextLine());
        if (inter >= 1) break;
        else System.out.print(inputError);
      } catch (IllegalArgumentException e) {
        System.out.print(inputError);
      }
    } while (true);

    clear.clear();
    CircularQueue cq = new CircularQueue(numberOfRoads, inter);
    QueueThread qt = new QueueThread(numberOfRoads, inter, cq);
    qt.start();
    qt.setEnumState(QueueThread.EnState.MENU);

    while (true) {
      System.out.println("""
                Menu:
                1. Add
                2. Delete
                3. System
                0. Quit""");
      String value = s.nextLine();

      switch (value) {
        case "1" -> {
          if (!cq.isFull()) {
            numOfInputRoads++;
          }
          System.out.print("Input road name: ");
          String name = s.nextLine();
          cq.enQueue(name, numOfInputRoads);
          continue;
        }
        case "2" -> {
          if (!cq.isEmpty()) numOfInputRoads--;
          cq.deQueue();
          continue;
        }
        case "3" -> {
          qt.setNumOfInputtedRoads(numOfInputRoads);
          qt.getArray(cq.giveQueueArray());
          qt.setEnumState(QueueThread.EnState.SYSTEM);
          System.in.read();
          qt.setEnumState(QueueThread.EnState.MENU);
          continue;
        }
        case "0" -> {
          System.out.println("Bye!");
          qt.setEnumState(QueueThread.EnState.NOT_STARTED);
        }
        default -> System.out.println(error);
      }
      if (value.equals("0")) break;
      s.nextLine();
      clear.clear();
    }
  }
}

class ClearScreen {
  public void clear() {
    try {
      var clearCommand = System.getProperty("os.name").contains("Windows")
              ? new ProcessBuilder("cmd", "/c", "cls")
              : new ProcessBuilder("clear");
      clearCommand.inheritIO().start().waitFor();
    } catch (IOException | InterruptedException e) {
      System.out.println("something goes wrong " + e);
    }
  }
}

class QueueThread extends Thread {
  public enum EnState {
    NOT_STARTED, MENU, SYSTEM
  }

  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_RESET = "\u001B[0m";
  volatile private int timer = 0;
  private final int num;
  private final int inter;
  volatile private String[] queueArray;
  volatile private int numOfInputtedRoads;
  EnState SysState;
  CircularQueue circularQueue;

  public QueueThread(int num, int inter, CircularQueue circularQueue) {
    super("QueueThread");
    this.circularQueue = circularQueue;
    this.SysState = EnState.NOT_STARTED;
    this.num = num;
    this.inter = inter;
  }

  public void setEnumState(EnState state) {
    this.SysState = state;
  }

  public void setNumOfInputtedRoads(int numOfInputtedRoads) {
    this.numOfInputtedRoads = numOfInputtedRoads;
  }

  synchronized public void getArray(String[] arr) {
    this.queueArray = arr;
  }

  synchronized public void getSek(int num, int inter, String[] arr, int[] roadTimers, CircularQueue.RoadState[] roadState) {
    System.out.printf("! %ds. have passed since system startup !\n", timer);
    System.out.printf("! Number of roads: %d !\n", num);
    System.out.printf("! Interval: %d !\n", inter);
    for (int i = 0; i < arr.length; i++) {
      if (circularQueue.isEmpty()) {
      } else if (!arr[i].isEmpty()) {
        String roadStatus = roadState[i] == CircularQueue.RoadState.OPEN ? "open" : "closed";
        String color = roadState[i] == CircularQueue.RoadState.OPEN ? ANSI_GREEN : ANSI_RED;
        int timeToChange = roadTimers[i];
        System.out.printf("%s%s will be %s for %ds.%s\n", color, arr[i], roadStatus, timeToChange, ANSI_RESET);
      }
    }
    System.out.println("! Press \"Enter\" to open menu !");
  }

  public void printMenu(int num, int inter) {

    System.out.printf("! %ds. have passed since system startup !\n", timer);
    System.out.printf("! Number of roads: %d !\n", num);
    System.out.printf("! Interval: %d !\n", inter);
    System.out.println("! Press \"Enter\" to open menu !");
  }

  @Override
  public void run() {
    while (!isInterrupted()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      this.timer++;
      if (this.SysState == EnState.SYSTEM) {
        if (numOfInputtedRoads == 0) printMenu(num, inter);
        else {
          getSek(num, inter, this.queueArray, circularQueue.giveRoadTimers(), circularQueue.getStateArray());
          circularQueue.upgradeRoadTimers(numOfInputtedRoads);
        }
      } else if (this.SysState == EnState.NOT_STARTED) {
        break;
      } else {
        this.SysState = EnState.MENU;
      }
    }
  }
}

class CircularQueue {
  public enum RoadState {
    OPEN, CLOSED
  }

  private int openRoadIndex = 0;
  private int nextOpenRoad = openRoadIndex + 1;
  private final int SIZE;
  private final int INTERVAL;
  private int front, rear;
  private String[] value;

  private RoadState[] stateArray;
  private int[] roadTimers;

  public CircularQueue(int size, int interval) {
    this.SIZE = size;
    this.INTERVAL = interval;
    this.value = new String[size];
    this.stateArray = new RoadState[size];
    this.roadTimers = new int[size];
    Arrays.fill(this.value, "");
    Arrays.fill(this.roadTimers, INTERVAL);
    this.front = -1;
    this.rear = -1;
  }

  // Check if the queue is full
  public boolean isFull() {
    if (front == 0 && rear == SIZE - 1) return true;
    if (front == rear + 1) return true;
    return false;
  }

  // Check if the queue is empty
  public boolean isEmpty() {
    return front == -1;
  }

  public RoadState[] getStateArray() {
    return stateArray;
  }

  public void setStateArray(RoadState[] stateArray) {
    this.stateArray = stateArray;
  }

  // Adding an element
  public void enQueue(String name, int roads) throws IOException {
    // Check if the queue is full
    if (isFull()) {
      System.out.println("queue is full");
      System.in.read();
    } else if (front == -1) {
      // If front is -1, the queue is empty
      front = 0;  // Set front to 0
      rear = (rear + 1) % SIZE;  // Increment rear
      value[rear] = name;
      openRoadIndex = 0;

      stateArray[openRoadIndex] = RoadState.OPEN;
      System.out.printf("%s Added!%n", name);
      System.in.read();  // Wait for the Enter key to be pressed
    } else {
      // If the queue is not empty and not full
      rear = (rear + 1) % SIZE;  // Increment rear
      value[rear] = name;  // Assign the road name to the appropriate position in the array
      System.out.printf("%s Added!%n", name);  // Display a message
      stateArray[rear] = RoadState.CLOSED;
      if (roads > 2) roadTimers[rear] = roadTimers[rear - 1] + INTERVAL;
      else roadTimers[rear] = roadTimers[rear - 1];
      System.in.read();
    }
  }

  // Removing an element
  public void deQueue() throws IOException {
    String road;
    if (isEmpty()) {
      System.out.print("queue is empty\n");
      System.in.read();
    } else {
      road = value[front];
      System.out.printf("%s deleted!%n", road);
      stateArray[front] = RoadState.CLOSED;
      value[front] = "";
      if (front == rear) {
        this.rear = -1;
        this.front = -1;
        openRoadIndex = -1; //
      } else {
        front = (front + 1) % SIZE;
        if (front == nextOpenRoad) {
          stateArray[front] = RoadState.OPEN;
        }
//        roadTimers[front] = INTERVAL;
      }
      System.in.read();
    }
  }

  public void upgradeRoadTimers(int roads) {
//    Queue<Integer> openRoadQueue = new LinkedList<>();
    for (int i = 0; i < roadTimers.length; i++) {
      if (roadTimers[i] > 1) {
        roadTimers[i]--;
      } else if (roadTimers[i] == 1) {
        if (roads == 1) {
          roadTimers[i] = INTERVAL;
          stateArray[i] = RoadState.OPEN;
        }
        if (roads > 1) {
          // Jeśli droga jest otwarta, zamknij ją, a jeśli jest zamknięta, otwórz ją
          if (stateArray[i] == RoadState.OPEN) {
            stateArray[i] = RoadState.CLOSED;
            roadTimers[i] = (roads - 1) * INTERVAL;
          } else {
            stateArray[i] = RoadState.OPEN;
            roadTimers[i] = INTERVAL;
//          openRoadQueue.add(i); // Dodaj indeks drogi do kolejki otwartych dróg
          }
        }
      }
    }
//     Obsłuż otwieranie dróg w kolejności z kolejki
//    while (!openRoadQueue.isEmpty()) {
//      int roadIndex = openRoadQueue.poll();
//      stateArray[roadIndex] = RoadState.OPEN;
//    }
  }

  public String[] giveQueueArray() {
    return this.value;
  }

  public int[] giveRoadTimers() {
    return roadTimers;
  }
}

