import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import static java.lang.System.exit;

public class Main {
    private static final int Q_LIMIT = 100;
    private static final int BUSY = 1;
    private static final int IDLE = 0;
    private static final float[] time_arrival = new float[Q_LIMIT + 1];
    private static final float[] time_next_event = new float[3];
    static FileInputStream reader;
    static PrintWriter writer;
    private static int next_event_type;
    private static int num_customers_delayed;
    private static int num_events;
    private static int num_in_queue;
    private static int server_status;
    private static float area_num_in_queue, area_server_status, mean_inter_arrival, mean_service_time, sim_time, time_last_event, total_delays;

    public static void main(String[] args) throws IOException {

        reader = new FileInputStream("/home/arapkering/Downloads/Single-Server-Simulation/src/mm1.in");
        Scanner scanner = new Scanner(reader);

        writer = new PrintWriter("/home/arapkering/Downloads/Single-Server-Simulation/src/mm1.out");

        /**
         * there are only two events in this system , departure and arrival.
         */

        num_events = 2;

        mean_inter_arrival = Float.parseFloat(scanner.next());
        mean_service_time = Float.parseFloat(scanner.next());
        int num_delays_required = Integer.parseInt(scanner.next());

        writer.println("Single-server queueing Simulation Statistics\n\n");
        writer.printf("Mean inter_arrival_time %11.3f minutes\n\n", mean_inter_arrival);
        writer.printf("Mean service time %16.3f minutes\n\n", mean_service_time);
        writer.printf("Number of customers %14d\n\n", num_delays_required);



       initialize();

        while (num_customers_delayed < num_delays_required) {
            /**
             * the timing() function determines the next event , the  update_time_avg_stats(); updates the statistics for the
             * simulation :
             *     time_last_event = sim_time;
             *     area_num_in_queue += num_in_queue * time_since_last_event;
             *     area_server_status += server_status * time_since_last_event;
             */

            timing();
            update_time_avg_stats();

            switch (next_event_type) {
                case 1 -> arrive();
                case 2 -> depart();
            }
        }

        report();

        /**
         * close the streams(consume the whole stream,  then close ) to avoid resource/data leakage .
         */

        reader.close();
        writer.close();

    }

    /**
     * the function initializes the simulation Clock , the STATE variables , and the STATISTICAL counters .
     * the function  also specifies  the time of the  first arrival . we don't consider the departure time since
     * there is no customer in service or in queue at the time .The event list second element(arrival time:time_next_event[2] )
     * is set  to 10^30.
     */

    public static void initialize() {
        sim_time = 0.0F;

        server_status = IDLE;
        num_in_queue = 0;
        time_last_event = 0.0F;

        num_customers_delayed = 0;
        total_delays = 0.0F;
        area_num_in_queue = 0.0F;
        area_server_status = 0.0F;

        time_next_event[1] = sim_time + expon(mean_inter_arrival); //0.4

        time_next_event[2] = 1.0e+30F;

    }

    /**
     * .This function here  Determines the event-type of the next event to occur.0 for arrival , 1 for departure
     */
    public static void timing() {

        float min_time_next_event = 1.0e+29F;
        System.out.println(min_time_next_event);
        next_event_type = 0;

        for (int i = 1; i <= num_events; ++i) {

            /**
             * Assume : arrival time is : 0.4 , and the departure  time is 10^30, and  time min_time_next_event is 10^29.
             * By this way , the arrival will always come as the first .no matter what .
             */

            if (time_next_event[i] < min_time_next_event) {
                min_time_next_event = time_next_event[i];
                next_event_type = i;

                if(next_event_type==1){
                    System.out.println("Arrival : " + min_time_next_event);

                }else {
                    System.out.println("Depature : "+ min_time_next_event);
                }
            }
        }
        if (next_event_type == 0) {
            writer.println("The Event list was Empty at time : " + sim_time);
            exit(0);
        }

//        Event list is not empty, simulation clock is advanced
        sim_time = min_time_next_event;
    }

    public static float expon(float mean) {
        return (float) (-mean * Math.log(Math.random()));
    }

    /**
     * The depart()  function here performs the  following :
     * if the queue is empty , then set the server status to IDLE.
     * If the queue is not empty ,so decrement the number of customers in the queue(another customer goes to service)
     * we Then compute the delay for the customer who is starting service.(then updates the total_delay accumulator)
     * increment the number of customers delayed ,and schedule departure.
     * Move each customer one step towards the front of the queue (if any).

     */
    public static void depart() {
        int i;
        float delay;

        if (num_in_queue == 0) {
            server_status = IDLE;
            time_next_event[2] = 1.0e+30F;
        } else if (num_in_queue<Q_LIMIT) {
            --num_in_queue;
            delay = sim_time - time_arrival[1];
            total_delays += delay;
            ++num_customers_delayed;
            time_next_event[2] = sim_time + expon(mean_service_time);
            /**
             * shift the arrival time of a customer one step towards the front of  the queue.
             */
            for (i = 1; i <= num_in_queue; ++i)
                time_arrival[i] = time_arrival[i + 1];

        }

    }

    /**
     *The function schedules the next arrival , and the next departure : That is ,  it populates these values to the
     * next_time_event array [_, arrival_time , departure_time ]
     */

    public static void arrive() {

        float delay;
        time_next_event[1] = sim_time + expon(mean_inter_arrival);

        if (server_status == BUSY) {
            ++num_in_queue;

            if (num_in_queue < Q_LIMIT) {
                /**
                 * The time_arrival is an array that can hold a maximum  of 100 arrival times.
                 * note  that the time of arrival is unique to a customer , and cna be used
                 */
                time_arrival[num_in_queue] = sim_time;

            } else if (num_in_queue>Q_LIMIT) {
                writer.println("\n The queue capacity was overflowed at ");
                writer.printf("time : %f", sim_time);
                exit(1);
            }

        } else {

            /**
             * If the Server is idle, the arriving customer has a delay of zero.The number of customers delayed is incremented,
             * but the delay for this customer is 0, so it won't affect  the total total_delay.
             */

            delay = 0.0F;
            total_delays += delay;
            ++num_customers_delayed;
            server_status = BUSY;
//            Schedule a departure (Service Completion)
            time_next_event[2] = sim_time + expon(mean_service_time);
        }
    }

    /**
     * The function does not override the contents of the mm1.out file but appends to it
     */

    public static void report() {

        writer.printf("\n\nAverage delay in queue %11.3f minutes\n\n", (total_delays / num_customers_delayed));
        writer.printf("Average number in queue%10.3f\n\n", area_num_in_queue / sim_time);
        writer.printf("Server utilization%15.3f\n\n", area_server_status / sim_time);
        writer.printf("Time simulation ended%12.3f minutes", sim_time);

    }
    public static void update_time_avg_stats() {

        float time_since_last_event;
        time_since_last_event = sim_time - time_last_event;
        time_last_event = sim_time;

        /**
         * Updates  teh area under the Number_InQueue<->Sim_Time graph , and  the area  for the server utilization graph
         */

        area_num_in_queue += num_in_queue * time_since_last_event;
        area_server_status += server_status * time_since_last_event;
    }
}