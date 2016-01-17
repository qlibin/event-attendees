Data model.
1.1.
There are N = 10000 events. Each event has {int id, int startTime} and an
unordered set of attendees.
1.2.
Event attendee is an int in range 1..M, M = 10.
1.3.
There may be K = 5 attendees for each event at most.

Execution mode.
2.1.
There are two sets of threads: Writers (W = 10), Readers (R = 10).
2.2.
Each writers in a loop picks random event id = 1..N and updates corresponding
event, or creates if no such found. When creating or updating, writer modifies
startTime to some random value in range 1..T, T = 100000 and attendee set to
random subset of 1..M. of max size K.
2.3.
Writer example:
Write #1. id = 123, startTime = 12030, attendees = {1, 3, 4, 6},
Write #2. id = 234, startTime = 3540,  attendees = {},
Write #3. id = 567, startTime = 75640, attendees = {7, 8}
2.4.
Each reader in loop picks random time range t1 < t2,  t1, t2 <= T, and random
subset of attendees and finds all events with startTime in the selected range and
which attendee set contains all selected attendees.
2.5.
Reader example:
Read #1. startTime: [10000, 20000], attendees = {3, 4}. Returns: id = 123.
Read #2. startTime: [0, 20000],     attendees = {}.  Returns: id = 234, 123.

Conditions.
3.1.
The program should also work for larger values of N, M. Like N = 100,000,000, M
= 10,000,000.
Questions.
4.1.
How would you store sets of attendees in the database?
4.2.
Provided that there may be lots of duplicated attendee sets, how would you
optimize storage needed to start attendees.
4.3.
How would you avoid deadlock while modifying existing events?
Implementation.
5.1.
The program should be implemented in Java with MySQL for storage.
5.2.
You may use hibernate or other ORM.
5.3.
Every 3 seconds program should print how many reads and writes it does per
second.