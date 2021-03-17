import com.mongodb.client.*;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.bson.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrainStation extends Application {
    static final int SEAT_NUMBER = 42;  //create a Global constant
    private static ArrayList<Passenger> waitingRoom = new ArrayList<>();    //waiting room to hold passengers
    private static ArrayList<Passenger> line = new ArrayList<>();

    //loadDataFromFile(seat);
    private static void loadDataFromFile(){
        String[][][] seat = new String[2][42][2];
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        // Accessing the database
        MongoDatabase database = mongoClient.getDatabase("TrainBooking");
        MongoCollection<Document> collection = database.getCollection("BookingRecords"); //get a collection
        System.out.println("Connected to the database successfully");
        FindIterable<Document> data = collection.find();
        for(Document temp :data){
            int trip=temp.getInteger("trip");
            seat[trip][temp.getInteger("seatNumber")-1][0]=temp.getString("name");
            seat[trip][temp.getInteger("seatNumber")-1][1]=temp.getString("nic");
        }
        System.out.println("Data imported successfully");

        for (int x=0;x<SEAT_NUMBER;x++){
            if (seat[0][x][0] != null){
                Passenger passenger = new Passenger();      // create a passenger object for each passenger
                passenger.setName(seat[0][x][0]);           // set name
                passenger.setSeat(x);                       // set seat
                waitingRoom.add(passenger);                 // add to waiting room
            }
        }
        System.out.println("waitingRoom size - "+waitingRoom.size());
    }
     
    public static void main(String[] args) {
        loadDataFromFile();     //data is loaded first
        Application.launch();
    } //call the start method

    @Override
    public void start(Stage primaryStage) {
        Scanner scan = new Scanner(System.in);
        String green="\033[1;92m"; //for color in menu
        String cyan="\033[0;96m";
        String def = "\u001B[0m";


        System.out.println(green +"\n----------Welcome to Train Booking System------------" + def);
        System.out.println("-----------------------------------------------------");
        menu:
        while (true) {
            System.out.println(cyan +"\n|Enter \"A\" add a passenger to the trainQueue        |"+ def);
            System.out.println(cyan +"|Enter \"V\" for view the trainQueue                  |"+ def);
            System.out.println(cyan +"|Enter \"D\" for Delete passenger from the trainQueue |"+ def);
            System.out.println(cyan +"|Enter \"S\" for Store trainQueue data                |"+ def);
            System.out.println(cyan +"|Enter \"L\" for Load trainQueue data                 |"+ def);
            System.out.println(cyan +"|Enter \"R\" for Run the simulation and produce report|"+ def);
            System.out.println(cyan +"|Enter \"Q\" for Quit                                 |"+ def);
            System.out.println(cyan +"-----------------------------------------------------"+ def);
            System.out.print(cyan +"Please select your option: "+ def);
            String selection = scan.nextLine();
            selection = selection.toUpperCase(); //convert user input to upper case
            switch (selection) {
                case "A":
                    while (true){
                        if(!addPassenger()) { break;}    //only will terminate if false is returned otherwise repeat
                    }
                    break;
                case "V":
                    view_TrainQueue();
                    break;
                case "D":
                    delete_Passenger_From_The_TrainQueue();
                    break;
                case "S":
                    store_trainQueue_data();
                    break;
                case "L":
                    getTrainQueuedata();
                    break;
                case "R":
                    run_The_Simulation();
                    break;
                case "Q":
                    System.out.println("---------------------------------------------------");
                    System.out.println("----------------Enjoy your Trip--------------------");
                    break menu; //quit from the menu
                default: //if no matching cases run
                    System.out.println("Invalid selection.Please check your input.");
                    break;
            }
        }
    }

    public static Boolean addPassenger() {
        AtomicBoolean control = new AtomicBoolean(false);   //atomic boolean to use in lambda expressions
        if (waitingRoom.size() == 0 && line.size() == 0) {     //if no data left or not imported
            System.out.println("No data in waiting room");
            return false;       //to break loop
        } else if (PassengerQueue.isFull()) {                 //check if queue is full
            System.out.println("passenger queue is full");
            return false;
        } else {
            if (line.size() == 0) {   //refill line with random number of passengers from the waitingRoom only if empty
                int random = ThreadLocalRandom.current().nextInt(1, 7);
                for (int x=0;x<random;x++) {
                    if (waitingRoom.size() == 0) break;
                    line.add(waitingRoom.remove(0));
                }
            }
            System.out.println("Line - "+line.size());
            VBox vBox = new VBox();
            HBox hBox1 = new HBox();
            HBox hBox2 = new HBox();
            Stage stage = new Stage();
            ArrayList<Button> lineList = new ArrayList<>();
            for (Passenger passenger : line) {
                Button lineBtn = new Button(passenger.getName()+" "+(passenger.getSeat()+1));
                lineBtn.setMouseTransparent(true);
                lineList.add(lineBtn);
                hBox1.getChildren().add(lineBtn);
            }
            System.out.println("LineList - "+lineList.size());
            lineList.get(0).setStyle("-fx-background-color:#ccfff5;");  //set color to show the selected passenger
            Button present = new Button("Present");
            Button absent = new Button("Absent");
            hBox2.getChildren().addAll(present, absent);
            present.setOnAction(event -> {
                if (PassengerQueue.isFull()) {                      //check if queue is full
                    System.out.println("passenger queue is full");
                    stage.close();
                }else {
                    lineList.get(0).setStyle("-fx-background-color:green;");
                    lineList.remove(0);                              //remove button
                    PassengerQueue.addToQueue(line.remove(0));      //add relevant passenger to queue
                    if (line.size() == 0) {                           //if line is empty
                        control.set(true);                            //to get this part to repeat
                        stage.close();
                    } else lineList.get(0).setStyle("-fx-background-color:#ccfff5;");  // show next button as selected
                }
            });
            absent.setOnAction(event -> {
                lineList.get(0).setStyle("-fx-background-color:red;");
                lineList.remove(0);
                line.remove(0);     //remove passenger but not adding to queue
                if (line.size() == 0) {
                    control.set(true);
                    stage.close();
                }else lineList.get(0).setStyle("-fx-background-color:#ccfff5;");
            });

            Label title = new Label("Add to TrainQueue");
            title.setLayoutX(80);
            title.setLayoutY(50);
            title.setFont(Font.font("Eras Demi ITC", 25));

            hBox1.setSpacing(20);
            hBox2.setSpacing(25);
            vBox.setSpacing(20);
            hBox2.setAlignment(Pos.CENTER);
            vBox.getChildren().addAll(hBox1,hBox2,title);
            Scene scene = new Scene(vBox);
            stage.setScene(scene);
            stage.showAndWait();
        }
        return control.get();
    }

    static void view_TrainQueue() {
        VBox waiting = new VBox();
        VBox lineQueue = new VBox();
        GridPane passengerQueue = new GridPane();
        for(Passenger passenger : waitingRoom){
            Button btn = new Button(passenger.getName()+" "+(passenger.getSeat()+1));
            btn.setStyle("-fx-background-color:#66cc99");
            waiting.getChildren().add(btn);
        }
        Label lb2 = new Label("Waiting Room");
        lb2.setLayoutX(60);
        lb2.setLayoutY(20);
        lb2.setFont(Font.font("Eras Demi ITC", 20));
        waiting.setStyle("-fx-background-color:#4dffff");
        waiting.setSpacing(10);
        waiting.getChildren().add(lb2);

        for(Passenger passenger : line){
            Button btn = new Button(passenger.getName()+" "+(passenger.getSeat()+1));
            lineQueue.getChildren().add(btn);
        }


        int x=0;
        int y=0;
        for(int i=0;i<42;i++){
            Button btn;
            if (PassengerQueue.getPassengerQueue()[i]!=null) {
                Passenger passenger = PassengerQueue.getPassengerQueue()[i];
                btn = new Button(passenger.getName() + " " + (i+1));
                btn.setStyle("-fx-background-color:#009900");
            }else {
                btn = new Button("Empty " + (i+1));

            }
            passengerQueue.add(btn,x,y);
            x++;
            if (x==6){
                y++;
                x=0;
            }
        }
        passengerQueue.setAlignment(Pos.CENTER);
        Label title = new Label("T"+"\nr"+"\na"+"\ni"+"\nn"+"\n\nQ"+"\nu"+"\ne"+"\nu"+"\ne");
        title.setLayoutX(80);
        title.setLayoutY(100);
        title.setFont(Font.font("Eras Demi ITC", 25));

        HBox hBox = new HBox(waiting,lineQueue,passengerQueue,title);
        hBox.setStyle("-fx-background-color:#99ffdd");
        hBox.setSpacing(100);
        Scene scene = new Scene(hBox);
        Stage stage =  new Stage();
        stage.setScene(scene);
        stage.showAndWait();
    }

    static void delete_Passenger_From_The_TrainQueue() {
        while (true) {
            Scanner input = new Scanner(System.in);
            System.out.print("Enter seat number to delete or -1 to exit - ");
            int deleteMe;
            try {
                deleteMe = input.nextInt()-1;
            } catch (InputMismatchException e) {
                System.out.println("Enter a number");
                continue;
            }
            if (deleteMe==-2) break;
            else if (deleteMe<0 || deleteMe>41){
                System.out.println("Invalid int");
                continue;
            }
            Passenger passenger = PassengerQueue.getPassengerQueue()[deleteMe];
            if (passenger==null){       //check if a passenger available to delete
                System.out.println("No passenger to delete");
            }else {
                System.out.println(passenger.getName()+" in "+(passenger.getSeat()+1)+" confirm(Y/N) ?");
                Scanner temp = new Scanner(System.in);
                String tempString = temp.next();
                if (tempString.equalsIgnoreCase("y")){
                    PassengerQueue.remove(deleteMe);    //call delete method in passenger queue only after confirming
                    System.out.println("Record deleted");
                }else System.out.println("Deletion canceled");
            }
        }
    }

    static void store_trainQueue_data() {
        if (PassengerQueue.isEmpty()) { //return if queue is empty
            System.out.println("No data");
            return;
        }
        try {
            FileOutputStream fileOut = new FileOutputStream("./data.txt");
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            for (Passenger passenger : PassengerQueue.getPassengerQueue()) {
                if (passenger != null) {
                    objectOut.writeObject(passenger);   //write objects to a file named data.txt
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not Found");
        } catch (IOException e) {
            System.out.println("File corrupt");
        }
        System.out.println("saved successfully");
    }

    static void getTrainQueuedata(){
        try {
            FileInputStream fi = new FileInputStream(new File("./data.txt"));
            ObjectInputStream oi = new ObjectInputStream(fi);
            while (true) {
                try {
                    Passenger passenger = (Passenger) oi.readObject();  //read objects from the text file and type cast to a passenger object
                    PassengerQueue.addToQueue(passenger);
                }catch (EOFException e) {break;}    //break after reaching end of file (EOF) end of file
            }
        }catch (FileNotFoundException e) {
            System.out.println("File not Found");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("File corrupt");
        }
        System.out.println("Loaded Successfully");
    }

    static void run_The_Simulation() {
        if (PassengerQueue.isEmpty()) { //return if queue is empty
            System.out.println("No data");
            return;
        }
        ArrayList<Passenger> tempQueue = new ArrayList<>();
        ArrayList<Passenger> queueLine = new ArrayList<>();
        for (int i=0; i<42; i++){       //get data from queue and delete them from queue
            if (PassengerQueue.getPassengerQueue()[i]!=null){
                tempQueue.add(PassengerQueue.getPassengerQueue()[i]);
                PassengerQueue.remove(i);
            }
        }
        int maxLine = 0;
        int count = 0;
        float totTime = 0;
        while (tempQueue.size()!=0 || queueLine.size()!=0) {        // run until all data is removed
            if (queueLine.size() == 0) {    // fill random number of passengers to an arrayList
                System.out.println("-----");
                int random = ThreadLocalRandom.current().nextInt(1, 7);
                if (tempQueue.size()<random) random=tempQueue.size();
                if (maxLine<random) maxLine=random; //update variable for max line length
                for (int x=0;x<random;x++) {
                    queueLine.add(tempQueue.remove(0));
                }
            }
            int randomTime = ThreadLocalRandom.current().nextInt(3, 19);
            for (Passenger passenger : queueLine){
                passenger.setSecondsInQueue(passenger.getSecondsInQueue()+randomTime);
            }   // time is added to all passengers in line
            System.out.println(queueLine.get(0).getSecondsInQueue());
            count++;    // count passengers
            totTime = totTime+queueLine.get(0).getSecondsInQueue(); // count total time for calculating average
            /*PassengerQueue.addToQueue(*/ queueLine.remove(0) ;//);    //remove first passenger only
        }
        String red="\033[1;91m";
        String def = "\u001B[0m";

        System.out.println(red+"Max Line len - "+ def +maxLine);
        System.out.println(red+"Total Passengers - "+def+count);
        System.out.println(red+"Average time in line - "+def+totTime/count);

        AnchorPane anchorPane = new AnchorPane();
        Stage stage = new Stage();

        Label title = new Label("The Simulation Details");
        title.setLayoutX(80);
        title.setLayoutY(50);
        title.setFont(Font.font("Eras Demi ITC", 25));

        Label maxline = new Label("Max Line len -\t"+maxLine);
        maxline.setLayoutX(80);
        maxline.setLayoutY(100);
        maxline.setFont(Font.font("Verdana", 16));

        Label Count = new Label("Total Passengers -\t"+count);
        Count.setLayoutX(80);
        Count.setLayoutY(140);
        Count.setFont(Font.font("Verdana", 16));

        Label average = new Label("Average time in line -\t"+totTime/count);
        average.setLayoutX(80);
        average.setLayoutY(180);
        average.setFont(Font.font("Verdana", 16));

        anchorPane.getChildren().addAll(maxline,Count,average,title);
        stage.setScene(new Scene(anchorPane, 400,300));
        stage.showAndWait();

        File file = new File("data.txt"); //should be simulation
        try {
            FileWriter fileWriter = new FileWriter(file,true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("\nMaximum Waiting Time :\t" + maxLine +
                    "\nTotal Passengers :\t" + (count) +
                    "\nAverage time in line :\t" + totTime/count);
            printWriter.close();
        } catch (IOException e) {
            System.out.println("File not found");
        } // display data and save to a file.
    }
}
