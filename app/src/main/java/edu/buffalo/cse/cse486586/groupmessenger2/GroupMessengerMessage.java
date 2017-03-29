package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Archita Pathak on 3/10/2017.
 */

public class GroupMessengerMessage implements Serializable {
    UUID msg_id;
    String msg_value;
    String msg_port;
    int msg_type; //0-> New, 1-> Proposed, 2->Agreed
    int msg_seq;

    public GroupMessengerMessage(UUID msg_id, String msg_value, String msg_port, int msg_type, int msg_seq) {
        this.msg_id = msg_id;
        this.msg_value = msg_value;
        this.msg_port = msg_port;
        this.msg_type = msg_type;
        this.msg_seq = msg_seq;
    }

    public UUID getMsg_id() {
        return msg_id;
    }

    public void setMsg_id(UUID msg_id) {
        this.msg_id = msg_id;
    }

    public String getMsg_value() {
        return msg_value;
    }

    public void setMsg_value(String msg_value) {
        this.msg_value = msg_value;
    }

    public String getMsg_port() {
        return msg_port;
    }

    public void setMsg_port(String msg_port) {
        this.msg_port = msg_port;
    }

    public int getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(int msg_type) {
        this.msg_type = msg_type;
    }

    public int getMsg_seq() {
        return msg_seq;
    }

    public void setMsg_seq(int msg_seq) {
        this.msg_seq = msg_seq;
    }

}
