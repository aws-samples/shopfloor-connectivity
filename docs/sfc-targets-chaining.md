# Target Chaining

- [Target chaining](#target-chaining)
- [Target chaining and buffering](#target-chaining-and-buffering)
  - [Store and forward target](#store-and-forward-target)
  - [Retention strategies](#retention-strategies)
- [Router Target](#router-target)




## Target chaining

To enable scenarios like store and forward, compression and encryption of data sent to targets, targets can now be
chained. Intermediate adapters can be placed in between the core and the adapters that deliver the data to the actual
destination. These intermediate targets are responsible for creating the adapter instances which are configured to
forward the data to.

When an intermediate target creates the target instances, it can optionally pass an implementation of the
TargetResultHandler interface. The instance of the created targets can use the instance of the passed interface
implementation to acknowledge, dis-acknowledge or report the forwarded data messages as failed back to the forwarding
intermediate target. The same interface can be used to query the data that the result of the forwarding target expects .
This can be just the serial number, the complete message or no data, for acknowledged, dis-acknowledged or error
messages.


## Target chaining and buffering

Targets receive data from the SFC core to deliver to a target-specific destination, which could be a local store, a local service, or a cloud service.To enhance the functionality of target data delivery, special intermediate targets can be configured between the sfc-core and the final targets. From the sfc-core's perspective, these intermediate targets appear as regular targets when writing data. 

However, they serve a unique purpose:

- Intermediate targets implement specific logic to process the received data. They then pass the processed data to the next targets in the chain. They provide a handler to subsequent targets for reporting delivery results.

- The data messages can be acknowledged if successfully delivered to the destination, not acknowledged if the destination was unavailable (e.g., due to connectivity loss), or reported as an error if the target couldn't process the data (e.g., due to invalid data format).Intermediate targets can then take appropriate actions based on the results received from the next targets in the chain.

This strategy allows for the addition of new functionalities in data delivery to target destinations without modifying the actual end-targets. It provides a flexible and modular approach to extending the capabilities of the SFC system's data delivery process.



<p align="center">
<img src="img/fig05.png" width="75%"/>


<p align="center">
    <em>Fig. 3. Example of target daisy-chaining</em>




Store and forwarding functionality for SFC targets is implemented using an intermediate target of type "store-forward-target". This target serves two main purposes:

- The store-forward-target buffers messages that could not be delivered to the destinations of the targets behind it. This decision is based on the results returned from these downstream targets. When the downstream targets can resume delivering data to their destinations, the store and forward target will resubmit the buffered data to these targets.

- This mechanism ensures that data is not lost when downstream targets are temporarily unable to deliver it, providing a robust solution for handling intermittent connectivity or destination unavailability issues in the SFC system.

  


## Store and forward target

As described above store and forwarding for SFC targets is implemented by an intermediate [store and forward  target](./targets/store-and-forward-target.md) that can be configured
in between the SFC-Core and the actual targets. This target stores the buffered data to disk if it cannot be delivered
to the destination of the targets that are configured as next targets in the chain.

Buffering will also take place in situation where the next targets in the chain are IPC targets which cannot be reached
by the store and forwarding targets die to network issues.

The store and forward target using to following logic:

- In normal situations the target will forward the target data to the next targets.
- For messages that can be delivered to their destinations these targets will send ACKs containing the serial number of
  the delivered messages.
- When the targets cannot deliver messages, NACKS, including the full message will be returned.
- When receiving NACKs the store and forward target will go into buffering mode and will start buffering data received
  by the core to disk.
- In buffering mode, the store and forward target will periodically send a buffered message, which is the oldest message
  that falls in the retention strategy (see below) of the buffer if the buffer is configured to operate in FIFO mode,
  which is the default. In LIFO mode the most recent message is used. An internal flag is set in the message to indicate
  to the target that this message should not be buffered but send directly to their destinations.
- The target will try to deliver this message to the destination and report an ACK or NACK for that message.
- When an ACK is received the store and forward target will switch back from buffering mode into normal mode after
  submitting the buffered data. This will happen in FIFO or LIFO mode based on configuration.
- Messages for which an ERROR is received are not stored and in case they are buffered removed from the store as this
  means they cannot be processed by the target.


## Retention strategies

In order to prevent running out of disk space of the device that is used to store the buffered messages a retention
strategy must be defined for a store and forward target. This can either be a period in minutes, a number of messages
per target the total size in MB per target. Data in the buffer that falls outside the used retention criteria will not
be resubmitted and automatically deleted from the storage device.

In order to reduce the storage of buffered messages the target will try to use hard links for messages that need to be
stored for multiple end targets, if the file system of that device supports it.

*PLEASE NOTE*  
Storing messages to a physical device can reduce the throughput of the SFC deployment. It is strongly recommended to run
process that contains the store and forward target, in memory or as an IPC service, on a device that has a fast storage
device.

## Router Target

The [router target](./targets/router.md) can be used to forward data to one or more targets in a target chain. For each target an alternative
target can be configured to which the data is routed if that data cannot be written to its primary target.

Each primary target can also have a target configured to which the data is routed if it has been written successfully to
its primary target or the alternative target of its primary target,

Used cases for the router target are:

- *Bundling* of (compressed) message data over a network to a system on which a group of targets, running as external
  services, are hosted.

<p align="center">
<img src="img/fig11.png" width="50%"/>


<p align="center">
    <em>Fig. 11. SFC Router target - bundling data</em>



- *Routing* of data *to alternative targets* if data cannot be written to primary targets

<p align="center">
<img src="img/fig12.png" width="50%"/>


<p align="center">
    <em>Fig. 12. SFC Router target - failover target</em>



- *Routing* of data to a *success target* after it has been written to primary targets or their alternative targets. The
  success target can be used to archive delivered messages or a custom target van notify the source of the data that the
  data has been delivered.

<p align="center">
<img src="img/fig13.png" width="50%"/>


<p align="center">
    <em>Fig. 13. SFC Router target - routing to a final `success` target</em>



<p align="center">
<img src="img/fig14.png" width="50%"/>


<p align="center">
    <em>Fig. 14. SFC Router target - routing to a final `success` target</em>


