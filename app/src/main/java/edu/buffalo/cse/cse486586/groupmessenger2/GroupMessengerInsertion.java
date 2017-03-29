package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.UUID;

/**
 * Created by Archita Pathak on 3/10/2017.
 */

public class GroupMessengerInsertion {
    UUID msg_id;
    String msg_port;
    int msg_seq;
    boolean msg_insertionFlag;
    String msg_value;

    public GroupMessengerInsertion(UUID msg_id, String msg_port, int msg_seq, boolean msg_insertionFlag, String msg_value) {
        this.msg_id = msg_id;
        this.msg_port = msg_port;
        this.msg_seq = msg_seq;
        this.msg_insertionFlag = msg_insertionFlag;
        this.msg_value = msg_value;
    }

    public UUID getMsg_id() {
        return msg_id;
    }

    public void setMsg_id(UUID msg_id) {
        this.msg_id = msg_id;
    }

    public String getMsg_port() {
        return msg_port;
    }

    public void setMsg_port(String msg_port) {
        this.msg_port = msg_port;
    }

    public int getMsg_seq() {
        return msg_seq;
    }

    public void setMsg_seq(int msg_seq) {
        this.msg_seq = msg_seq;
    }

    public boolean isMsg_insertionFlag() {
        return msg_insertionFlag;
    }

    public void setMsg_insertionFlag(boolean msg_insertionFlag) {
        this.msg_insertionFlag = msg_insertionFlag;
    }

    public String getMsg_value() {
        return msg_value;
    }

    public void setMsg_value(String msg_value) {
        this.msg_value = msg_value;
    }


    public static Comparator<GroupMessengerInsertion> Comparator = new Comparator<GroupMessengerInsertion>() {
        @Override
        public int compare(GroupMessengerInsertion lhs, GroupMessengerInsertion rhs) {
            int lhsAgreed = lhs.getMsg_seq();
            int rhsAgreed = rhs.getMsg_seq();
            int lhsPort = Integer.parseInt(lhs.getMsg_port());
            int rhsPort = Integer.parseInt(rhs.getMsg_port());

            if(lhsAgreed == rhsAgreed)
                return lhsPort - rhsPort;
            else
                return lhsAgreed - rhsAgreed;
        }
    };
}
