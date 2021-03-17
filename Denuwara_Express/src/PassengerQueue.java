public class PassengerQueue {
    private static Passenger[] passengerQueue = new Passenger[42];

    public static void addToQueue(Passenger passenger){
        passengerQueue[passenger.getSeat()]=passenger;
    }   // add passenger according to seat number

    public static Passenger[] getPassengerQueue() {
        return passengerQueue;
    }   // return the whole queue

    public static void remove(int index) {
        passengerQueue[index]=null;
    }   // delete method

    public static boolean isEmpty() {
        for (Passenger passenger : passengerQueue){
            if (passenger != null) return false;
        }
        return true;    // only if empty
    }

    public static boolean isFull() {
        for (Passenger passenger : passengerQueue){
            if (passenger == null) return false;
        }
        return true;    // only if full
    }
}
