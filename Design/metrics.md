Intents
=======

* Has everything been healthy?
  * THEME: history, trends, configurable time range
  * {node,container} uptime
  * {node,container} count / bounces
  * {node,container} cpu maxed?
  * {node,container} mem maxed? (swapping, paging, ctx switching?)
  * node network connectivity (other forms of health?)
  * free space
  * free fds
  * average response time

* Is everything healthy now?
  * THEME: Current values, percentages: current/capacity, no history but minimal (1m) interval where required
  * {node,container} quantity up vs expected
  * {node,container} uptime
  * {node,container} cpu: load avg vs capacity
  * {node,container} mem: used vs capacity
  * {node,container} mem: thrashing/swapping (?)
  * {node,container} fds: used vs capacity (?)
  * disk space: used vs capacity
  * network: ... up? errors? although if currently down, then no metrics available until back online
  * logs: {warn,error} rate (maybe over 1/5/15 like load?)
  * average response time

* Is everything going to stay healthy? Is there any preemptive action to take?
  * THEME: trend/capacity
  * Disk space: free vs trend
  * Memory: free vs trend
  * CPU: capacity vs trend

* What's happening with/in Node/Container/X

* How is everything being used?
  * What kind of volume are we experiencing?
  * Breadth: how many requests, unique users
  * Depth: how many interactions/user



Thoughts
========

#### Webapp

- users
  - registered vs not
  - last login
- projects
  - total created & deleted
- requirements
  - total
  - per project
- events
  - total
  - per project
- disk space
  - total
  - per project
  - per user
  - per event
  - per requirement


Cheatsheet
==========

```
[C]ounter   - monotonic number; use for X per N (eg. req/sec)
[G]auge     - number
[H]istogram - stream of numbers, recorded in buckets
[S]ummary   - client-side quantiles, pre-determined window size
```

