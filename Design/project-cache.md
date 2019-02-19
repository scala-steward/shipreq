Actions:
* StartPage(userId, projectId)
* InitialProject(userId, projectId, seq)
* AddEvent(userId, projectId, seq)
* RecvEvent(userId, projectId, seqFrom, seqTo)

State:
* Redis
  * project, seq
  * [events] since project
* Postgres
  * events