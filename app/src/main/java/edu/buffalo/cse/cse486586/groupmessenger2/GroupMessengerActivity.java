package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;

import static java.lang.Thread.sleep;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORTS = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    private static ContentResolver mContentResolver;
    private static Uri mUri;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    public static ArrayList<String> livePorts = new ArrayList<String>();
    public static HashMap<UUID,Integer> senderCount = new HashMap<UUID, Integer>();
    public static HashMap<UUID,Integer> proposedSeq = new HashMap<UUID, Integer>();
    public static ReentrantLock lock = new ReentrantLock(true);
    public static HashMap<UUID,String> holdBackQueue = new HashMap<UUID,String>();
    public static volatile ArrayList<GroupMessengerInsertion> insertionQueue = new ArrayList<GroupMessengerInsertion>();
    static int SEQUENCE_NUMBER = 0;
    public static boolean failureFlag = false;
    int localSeqNo = -1;
    static String myPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        livePorts.add("11108");
        livePorts.add("11112");
        livePorts.add("11116");
        livePorts.add("11120");
        livePorts.add("11124");

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            ScheduledExecutorService ping = Executors.newScheduledThreadPool(1);
            ping.scheduleWithFixedDelay(new PingAck(),8,1,TimeUnit.SECONDS);
            ping.scheduleWithFixedDelay(new PingAck(),8,1,TimeUnit.SECONDS);


        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        //Initialize Content Resolver
        mContentResolver = getContentResolver();
        //Build URI
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //***************************************************************************************
                //Get EditText message
                String msg = editText.getText().toString() + "\n";
                editText.setText("");

                //Check if it is null or not
                if (!msg.isEmpty()) {
                    //Creating Async task for all 5 ports to send msgs to all AVDs
                    lock.lock();
                    GroupMessengerMessage initMessage = new GroupMessengerMessage(UUID.randomUUID(), msg, myPort, 0, -1);
                    lock.unlock();
                    if(initMessage!=null)
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, initMessage);
                }
                //***************************************************************************************
            }
        });
    }

    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private GroupMessengerMessage FormMessage (String msgString){
        String msgParts[] = msgString.split(":");
        GroupMessengerMessage msg = new GroupMessengerMessage(UUID.fromString(msgParts[0]), msgParts[1], msgParts[2],
                Integer.valueOf(msgParts[3]), Integer.valueOf(msgParts[4]));

        return msg;
    }
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket = null;
            DataInputStream inputStream = null;

            while(true) {
                try{
                    socket = serverSocket.accept();
                    GroupMessengerMessage msg = null;
                    inputStream = new DataInputStream(socket.getInputStream());
                    String msgString = inputStream.readUTF();

                    if(msgString.contains("Ping")){
                        //Ping Msg
                        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                        outputStream.writeUTF("PingAck");
                        outputStream.flush();
                        outputStream.close();

                    }
                    else
                        msg = FormMessage(msgString);

                    if(msg!=null && !msg.getMsg_value().isEmpty()) {


                        //New msg
                        if (msg.getMsg_type() == 0) {
                            localSeqNo++;
                            msg.setMsg_seq(localSeqNo);
                            Boolean foundFlag = false;
                            String id = msg.msg_id.toString().concat(msg.getMsg_port());
                            if(!holdBackQueue.containsKey(msg.msg_id)){
                                holdBackQueue.put(msg.msg_id,msg.msg_port);
                                synchronized (insertionQueue){
                                    insertionQueue.add(new GroupMessengerInsertion(msg.getMsg_id(), msg.getMsg_port(), msg.getMsg_seq(), false, msg.getMsg_value()));
                                }
                            }
                            //Sending Acknowledgement alogwith Proposed Seq. No. to client
                            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                            outputStream.writeUTF("Proposal:" + String.valueOf(localSeqNo));
                            outputStream.flush();
                            outputStream.close();
                        }

                        if (msg.getMsg_type() == 1) {
                            //Sending Acknowledgement
                            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                            outputStream.writeUTF("Agreed");
                            outputStream.flush();
                            outputStream.close();

                            synchronized (insertionQueue) {
                                for (GroupMessengerInsertion it : insertionQueue) {
                                    if ((it.getMsg_id().equals(msg.getMsg_id())) && (it.getMsg_port().equals(msg.getMsg_port()))) {
                                        it.setMsg_seq(msg.getMsg_seq());
                                        it.setMsg_insertionFlag(true);
                                        Collections.sort(insertionQueue, GroupMessengerInsertion.Comparator);
                                        break;
                                    }
                                }
                            }

                            //Displaying queue
                            Log.e("Displaying queue","...............................");
                            Iterator<GroupMessengerInsertion> dispIt = insertionQueue.iterator();
                            while (dispIt.hasNext()) {
                                GroupMessengerInsertion item = dispIt.next();
                                Log.e(item.getMsg_id() + " Seq no:" + String.valueOf(item.getMsg_seq()), String.valueOf(item.msg_insertionFlag) + " Port:" + item.getMsg_port());
                            }
                            Log.e("Displaying queue","...............................");

                            //5. Send for delivery
                            synchronized (insertionQueue) {
                                Iterator<GroupMessengerInsertion> iterator = insertionQueue.iterator();
                                while (iterator.hasNext()) {
                                    GroupMessengerInsertion msgToInsert = iterator.next();
                                    if (msgToInsert.isMsg_insertionFlag()) {
                                        publishProgress(msgToInsert.getMsg_value());
                                        localSeqNo = Math.max(msgToInsert.getMsg_seq(), localSeqNo);
                                        iterator.remove();
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                        inputStream.close();
                    }
                }
                catch (ConnectException e) {
                    Log.e(TAG, "ServerTask socket Exception: " + e.toString());
                    e.printStackTrace();

                } catch (EOFException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "ServerTask socket IOException: " + e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(TAG, "ServerTask socket Exception: " + e.toString());
                    e.printStackTrace();
                }
                finally
                {
                    if(socket!=null)
                    {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            Log.e(TAG, "Publishing");
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");

            //Inserting in Content Provider
            ContentValues contentValues = new ContentValues();
            contentValues.put(KEY_FIELD,SEQUENCE_NUMBER++);
            contentValues.put(VALUE_FIELD,strReceived);
            mContentResolver.insert(mUri,contentValues);
            Log.e(TAG, "Published");
            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }


    private class ClientTask extends AsyncTask<GroupMessengerMessage, Void, Void> {

        @Override
        protected Void doInBackground(GroupMessengerMessage... msgs) {
            String failedPort = null;
            Socket clientSocket = null;
            GroupMessengerMessage outMessage = msgs[0];
            DataInputStream inputStream = null;
            proposedSeq.put(outMessage.getMsg_id(),-1);
            Boolean continueFlag = true;


            String ack = "";
            while((outMessage.getMsg_type() == 0 || outMessage.getMsg_type() == 1) && continueFlag == true) {
                if (outMessage.getMsg_type() == 0) {
                    String msg = outMessage.msg_id.toString() + ":" + outMessage.msg_value + ":" + outMessage.msg_port + ":" +
                            String.valueOf(outMessage.msg_type) + ":" + String.valueOf(outMessage.msg_seq);
                    try {
                        for (String remotePort : REMOTE_PORTS) {
                            if (livePorts.contains(remotePort)) {

                                clientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(remotePort));
                                //clientSocket.setSoTimeout(7000);

                                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                                out.writeUTF(msg);
                                out.flush();
                                failedPort = remotePort;
                                //Waiting for server's acknowledgement
                                inputStream = new DataInputStream(clientSocket.getInputStream());
                                ack = inputStream.readUTF();
                                if (ack.contains("Proposal")) {
                                    int seqNo = Integer.valueOf(ack.substring(ack.indexOf(":") + 1));

                                    if (seqNo > proposedSeq.get(outMessage.getMsg_id()))
                                        proposedSeq.put(outMessage.getMsg_id(), seqNo);

                                    if (senderCount.get(outMessage.getMsg_id()) == null)
                                        senderCount.put(outMessage.getMsg_id(), 1);
                                    else
                                        senderCount.put(outMessage.getMsg_id(), senderCount.get(outMessage.getMsg_id()) + 1);

                                    inputStream.close();
                                    clientSocket.close();
                                }
                            }
                        }
                        if (senderCount.get(outMessage.getMsg_id()) != null && senderCount.get(outMessage.getMsg_id()) >= 4) {
                            outMessage.setMsg_type(1);
                            outMessage.setMsg_seq(proposedSeq.get(outMessage.getMsg_id()));
                        }

                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "ClientTask SocketTimeoutException");
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask IOException");
                        e.printStackTrace();
                    } finally {
                        if (clientSocket != null) {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }


                if (outMessage.getMsg_type() == 1) {
                    String msg = outMessage.msg_id.toString() + ":" + outMessage.msg_value + ":" + outMessage.msg_port + ":" +
                            String.valueOf(outMessage.msg_type) + ":" + String.valueOf(outMessage.msg_seq);
                    try {
                        for (String remotePort : REMOTE_PORTS) {
                            if (livePorts.contains(remotePort)) {

                                clientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(remotePort));
                                //clientSocket.setSoTimeout(7000);

                                DataOutputStream agreedout = new DataOutputStream(clientSocket.getOutputStream());
                                agreedout.writeUTF(msg);
                                agreedout.flush();
                                failedPort = remotePort;
                                //Waiting for server's acknowledgement
                                inputStream = new DataInputStream(clientSocket.getInputStream());
                                ack = inputStream.readUTF();
                                if (ack.contains("Agreed")) {
                                    inputStream.close();
                                    clientSocket.close();
                                }


                            }
                        }
                        continueFlag = false;

                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "ClientTask SocketTimeoutException");
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask IOException");
                        e.printStackTrace();
                    } finally {
                        if (clientSocket != null) {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if(continueFlag == false)
                    outMessage.setMsg_type(100);

            }
            return null;
        }
    }

    private void cleanUp(String msgPort) {
        if (msgPort != null) {
            Log.e("Cleaning up...........", msgPort);
            List<GroupMessengerInsertion> listToRemove = new ArrayList<GroupMessengerInsertion>();
            int count = 0;
            if (livePorts.size() == 5)
                livePorts.remove(msgPort);
            Iterator<GroupMessengerInsertion> iterator = insertionQueue.iterator();
            synchronized (insertionQueue) {
                while (iterator.hasNext()) {
                    GroupMessengerInsertion msgToClean = iterator.next();
                    if (msgToClean.getMsg_port().equals(msgPort) && msgToClean.isMsg_insertionFlag() == false) {
                        count++;
                        listToRemove.add(msgToClean);
                    }
                }
                insertionQueue.removeAll(listToRemove);
            }
            Log.e("Cleaning up...........", String.valueOf(count) + " messages deleted");
        }
    }

   private class PingAck implements Runnable{
        Socket clientSocket = null;

        @Override
        public void run() {
            String failedPort = "";
            try {
                for (String remotePort : livePorts) {
                    if (!remotePort.equals(myPort) && livePorts.size() == 5) {
                        clientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        //clientSocket.setSoTimeout(7000);


                        DataOutputStream pingOut = new DataOutputStream(clientSocket.getOutputStream());
                        pingOut.writeUTF("Ping");
                        pingOut.flush();

                        failedPort = remotePort;

                        //Waiting for server's acknowledgement
                        DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
                        String ack = inputStream.readUTF();

                        if (ack.contains("PingAck")) {

                            inputStream.close();
                            clientSocket.close();
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "PingTask SocketTimeoutException");
                e.printStackTrace();
            } catch (UnknownHostException e) {
                Log.e(TAG, "PingTask UnknownHostException");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "PingTask IOException");
                Log.e("PingAck","Cleaning.........................." + failedPort);
                cleanUp(failedPort);
                Log.e("PingAck","Cleaning done.........................." + failedPort);
                e.printStackTrace();
            } finally {
                if(clientSocket!=null)
                {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
