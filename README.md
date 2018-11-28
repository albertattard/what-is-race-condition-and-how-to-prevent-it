Say that you are eating some sweets while watching a movie.  You had a look at your pack or sweets, counted five and continued watching the movie.  Took the first one and deducted this in your mind (without looking at the sweets pack).  In the mean time, and without you knowing, your daughter takes one.  While you think that there are four sweets left, in reality there are only three.  You will discover this at the end when you are looking for the last one.  This is a Race Condition and in this article we will see how this occurs in programming and how we can avoid it.

The readers of this article are expected to have some background about threads as otherwise they may have difficulties to understand what is being discussed.  The readers are encouraged to read the article [Understanding Threads Monitors and Locks](http://www.javacreed.com/understanding-threads-monitors-and-locks/) to get a good understanding about locks and how these are used by threads and their role in multithreading.

All code listed below is available at: [https://github.com/javacreed/what-is-race-condition-and-how-to-prevent-it](https://github.com/javacreed/what-is-race-condition-and-how-to-prevent-it).  Most of the examples will not contain the whole code and may omit fragments which are not relevant to the example being discussed. The readers can download or view all code from the above link.

## What is a Race Condition

In concurrent programming a Race Condition occurs when a second thread modifies the state of one (or more objects), making any assumptions, checks, made by the first threads invalid.  This is sometimes referred to as "_check then act_".

Recall our pack of sweets example.  Two persons are involved in this example. The first person checked how many sweets there were in the pack and counted five.  Then he took one (leaving a balance of four).  The second person took one after the first person counted the sweets.  While the first person believes that there are four sweets, in reality there are three.  For this problem to manifests itself, we need at least two persons.  Not only that, we need to have a situation where the second person changes the state of the environment (number of sweets in this case) without the first person notice.  This needs to be performed after the first person checks and before he reaches the end (eats all sweets).

Another example of Race Condition is shopping online.  Say that you found an item you want to buy in an online store.  The online store indicates that they only have one left.  By the time you press buy, another user beats you to it and buys it after you browse the page (which page indicates one item left) but before you pressed buy.  This is another example of Race Condition as the state changed while you were shopping without you knowing.  Like before, for this condition to manifest itself, you need more than one users, purchasing the same item at the same time.

Race Condition applies to programming too.  Programs that make use of multiple threads are subject to Race Conditions if these are not properly designed.  This is best seen with an example.  Consider the following class.

```java
package com.javacreed.examples.concurrency.part1;

public class Data {

  private int value;

  public synchronized int getValue() {
    return value;
  }

  public synchronized void setValue(final int value) {
    this.value = value;
  }

}
```


The `Data` class shown above is quite straightforward and requires little explanation.  It has one field, called `value`, which field is accessed through the `getValue()` and `setValue()` methods.  These methods are `synchronized` which means that only one thread can access each method at one point in time.  Furthermore, the `synchronized` block will cause each thread accessing it to cross the memory barrier and always get actual value instead of using the cached copy.  Each thread keeps a cached copy of the variables used for performance reasons.  The `synchronized` forces the thread to refreshes its cache, thus making sure that the thread has the latest values.  Moreover, any changes made to the value are then copied to the main memory so that other threads can read the latest values.

One can safely say that this class is thread safe.

Following is an example of how we can increment the value of such object.

```java
package com.javacreed.examples.concurrency.part1;

public class Example1 {

  public static void main(final String[] args) {
    final Data data = new Data();

    final int value = data.getValue();
    data.setValue(value + 1);
    System.out.println(data.getValue());
  }
}
```

The value is first retrieved through the `getValue()` method.  This value is then incremented by one and then passed to the `setValue()` method to increment the state of the object by one.  This simple operation is subject to Race Condition when used within a multithreaded programming environment as shown in the next example.


```java
package com.javacreed.examples.concurrency.part1;

import java.util.ArrayList;
import java.util.List;

public class Example2 {

  public static void main(final String[] args) throws Exception {
    for (int run = 0, numberOfThreads = 100; run < 1000; run++) {
      System.out.printf("Run %05d.....", run + 1);
      final Data data = new Data();

      final List<Thread> threads = new ArrayList<>(numberOfThreads);
      for (int i = 0; i < numberOfThreads; i++) {
        final Thread thread = new Thread(new Runnable() {
          @Override
          public void run() {
            final int value = data.getValue();
            data.setValue(value + 1);
          }
        });
        thread.start();
        threads.add(thread);
      }

      for (final Thread thread : threads) {
        thread.join();
      }

      if (data.getValue() == numberOfThreads) {
        System.out.println("Passed");
      } else {
        System.out.printf("Failed with value %d instead of %d%n", data.getValue(), numberOfThreads);
        break;
      }
    }
  }
}
```

This example got a bit complex.  Let us break it down and analyse it further.

1. One of the main problems in concurrency is that the problem will not always manifest itself.  The problem will only show its face when certain conditions are met.  If we run the program once, we may not observe the problem.  Therefore we iterate for several times (one thousand in this case) or until the problem occurs.  This is controlled by the `run` variable which will cause the loop to stop after one thousand iterations.  The second variable, named `numberOfThreads` governs the number of threads to be used in the experiment.

    ```java
        for (int run = 0, numberOfThreads = 100; run < 1000; run++) {
          /* omitted for brevity */
        }
    ```

1. Create an instance of the `Data` class, which instance will be shared by all threads.  All thread that will be created and used within this example will work on this single instance of the `Data` class.

    ```java
          final Data data = new Data();
    ```

1. Creates a list which will contain all threads that are created during this example.   This will be used later on to wait for all thread to finish before proceeding further.

    ```java
          final List<Thread> threads = new ArrayList<>(numberOfThreads);
    ```

1. Create a number of threads, start each created thread and add it to the list.

    ```java
          for (int i = 0; i < numberOfThreads; i++) {
            final Thread thread = new Thread(new Runnable() {
              @Override
              public void run() {
                final int value = data.getValue();
                data.setValue(value + 1);
              }
            });
            thread.start();
            threads.add(thread);
    ```

    Each thread will simply increment the value of the `data` object by one.

    In this part of the code we could have used the `CountDownLatch` ([Java Doc](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CountDownLatch.html)) to make sure that all thread try to manipulate the state at the same time.  This will increase the probability of problem occurrence.  For simplicity, this was left out.

1. Wait for all threads to finish.

    ```java
          for (final Thread thread : threads) {
            thread.join();
          }
    ```

    This is important as otherwise we may get the wrong information in the next stage.

1. Verify that the final value is the equal to the number of threads.  The initial value is 0 and each thread increments this by one.  Therefore, at the end of the exercise this value should be equal to the number of threads that incremented it.

    ```java
          if (data.getValue() == numberOfThreads) {
            System.out.println("Passed");
          } else {
            System.out.printf("Failed with value %d instead of %d%n", data.getValue(), numberOfThreads);
            break;
          }
    ```

    A message is displayed showing whether the value is as expected or not.

Running the above class will produce different results and may fail at different stages.  The following output shows that in this case the test failed after six iterations.

```
Run 00001.....Passed
Run 00002.....Passed
Run 00003.....Passed
Run 00004.....Passed
Run 00005.....Passed
Run 00006.....Failed with value 99 instead of 100
```

In other cases it took more that seven hundred iterations to fail and each run will fail at different iteration.  While this never happened on my development environment, there can be cases where such test never fails.  This shows how unpredictable such problems are.

This is an example of a Race Condition as two threads read the same value at the same time, say both of them increment this value, and finally update the value, as shown next.

```
Thread 1 read value of 10
Thread 2 read value of 10
Thread 1 saves the value of 10 into a local variable
Thread 1 sets the value of 11 (adds 1 to the local variable with value of 10)
Thread 2 saves the value of 10 into a local variable
Thread 2 sets the value of 11 (adds 1 to the local variable with value of 10)
```

Both threads read the value at the same time.  Thus we lost the one of the increments as both threads left the with value of eleven.  This is a Race Condition as Thread 1 modified the state of the object between when Thread 2 read the state and when it acted on it.  In this case Thread 1 changed the state for Thread 2.

In this section we saw what Race Condition is, and how this is caused.  In the following section we will see how to address this.

## Addressing Race Condition

Once identified, Race Condition can be addressed in various ways.  In this section we will see two possible, very generic, solutions which are suitable for many cases.  Both solutions make use of the same concept, that is locks (through synchronization).  All access to the group of actions (or statements of code) is serialised and thus the threads need to take turns as the second one needs to wait for the first thread to finish.

In our example, we make the following actions:

1. Read the value

    ```java
                  final int value = data.getValue();
    ```

1. Set the value

    ```java
                  data.setValue(value + 1);
    ```

These two actions need to be execution in isolation and the object state cannot change by a second thread while the first thread is executing any of these actions.  The access to the `getValue()` and `setValue()` methods is serialised using the object intrinsic lock (or monitor lock [Tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html)).  One way to address this problem is to extend the lock to cover both methods without any interruptions as shown next.

```java
            synchronized (data) {
              final int value = data.getValue();
              data.setValue(value + 1);
            }
```

The intrinsic lock on the object instance now spans over both actions together and thus prevents a thread from entering this region before the previous thread finishes.  This solution addresses the race condition as this solution prevents the states from being invalidated by another thread (as long as all threads make use of this approach).

The following example will never fail.

```java
package com.javacreed.examples.concurrency.part2;

import java.util.ArrayList;
import java.util.List;

public class Example3 {

  public static void main(final String[] args) throws Exception {
    for (int run = 0, numberOfThreads = 100; run < 1000; run++) {
      System.out.printf("Run %05d.....", run + 1);
      final Data data = new Data();

      final List<Thread> threads = new ArrayList<>(numberOfThreads);
      for (int i = 0; i < numberOfThreads; i++) {
        final Thread thread = new Thread(new Runnable() {
          @Override
          public void run() {
            synchronized (data) {
              final int value = data.getValue();
              data.setValue(value + 1);
            }
          }
        });
        thread.start();
        threads.add(thread);
      }

      for (final Thread thread : threads) {
        thread.join();
      }

      if (data.getValue() == numberOfThreads) {
        System.out.println("Passed");
      } else {
        System.out.printf("Failed with value %d instead of %d%n", data.getValue(), numberOfThreads);
        break;
      }
    }
  }
}
```

The above will always reach the end and will print the following

```
...
Run 00997.....Passed
Run 00998.....Passed
Run 00999.....Passed
Run 01000.....Passed
```

The approach shown above is a bit risky.  It relies on the users (the developers using this code) to synchronize the access of these two actions (methods) as shown above.  Ideally this is done automatically without the user''s intervention.  The second approach provides a third method which encapsulated this logic as shown next.

```java
package com.javacreed.examples.concurrency.part3;

public class Data {

  private int value;

  public synchronized void adjustBy(final int adjustment) {
    value += adjustment;
  }

  /* Methods removed for brevity */
}
```

The `Data` class was modified and a new method was added.  The `adjustBy()` adjusts (increments or decrements) the value by the given adjustment within a synchronised block.  The coders can use this method to prevent Race Condition.  Like in the previous solution, we are preventing the state to change by a second thread while the first thread is working with it, thus making it invalid.

The following example shows the use of the new method.

```
package com.javacreed.examples.concurrency.part3;

import java.util.ArrayList;
import java.util.List;

public class Example4 {

  public static void main(final String[] args) throws Exception {
    for (int run = 0, numberOfThreads = 100; run < 1000; run++) {
      System.out.printf("Run %05d.....", run + 1);
      final Data data = new Data();

      final List<Thread> threads = new ArrayList<>(numberOfThreads);
      for (int i = 0; i < numberOfThreads; i++) {
        final Thread thread = new Thread(new Runnable() {
          @Override
          public void run() {
            data.adjustBy(1);
          }
        });
        thread.start();
        threads.add(thread);
      }

      for (final Thread thread : threads) {
        thread.join();
      }

      if (data.getValue() == numberOfThreads) {
        System.out.println("Passed");
      } else {
        System.out.printf("Failed with value %d instead of %d%n", data.getValue(), numberOfThreads);
        break;
      }
    }
  }
}
```

This solution is simpler as the users of the `Data` class do not have to worry about synchronization.  Like the previous one, this solution will never fail and will always produce the expected result.

## Conclusion

In this article we discussed Race Condition and how this can happen.  Then we discussed two solutions, the second of which is simpler to implement as it centralise all required changes into one method.  Like all concurrent problems, these are hard to replicate and may go unnoticed for a long time.  Different approaches can be applied and centralised solutions should be preferred over decentralised ones.  Class designers should think further and understand well how these classes are used especially within a multithreaded environment.  Like in our simple `Data` class, the third method `adjustBy()`, address a fundamental problem which was originally missed.
