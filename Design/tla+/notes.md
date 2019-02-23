first attempt: open/close tabs, user msgs, webapps, websockets etc.
remove easy (conn mgmt), irrelevant to specific problem...
what's my specific problem? I can manage out-of-order seqs & conn mgmt easily myself.
My problem is atomicity, cache invalidation, staleness, distributed txns wrt project caching/snapshots/event-processing

Ok Redis,DB,Webapps... but webapps are stateless so they don't matter,
Stateful things are: DB,Reids,usersStates
NO! Webapps have temporary state/req. It doesn't matter that it's a webapp, it's a req processor
Stateful things are: DB,Reids,userStates,processors

projects are independent, never conflict - it's trivial in code to separate them and accidentally using the wrong id is a typo, not something that requires a proof
therefore our model can be focused only on a single project
