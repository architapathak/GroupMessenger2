A group messenger with 5 android emulators (avd) and implemented Total-FIFO guarantees using a decentralized algorithm. All the messages were stored in SQLITE database as a key-value pair with key being their sequence number. B-multicast was used to multicast messages. The algorithm was designed to handle atmost one process failure which was crash-stopped.  Failure was detected using Ping-Ack failure detector and the failed process state was cleaned to continue smooth communications between live processes.

The template was provided by Prof. Steve Ko.
