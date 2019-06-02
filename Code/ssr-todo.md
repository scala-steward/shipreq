* Router in SSR tries to pushState/replaceState
  - let it crash and hide from logs?
  - or, have the router be SSR aware and....?

* need to render after hydration...
  - if window.location.hash is set (because server doesn't consider)
  - if localStorage isn't available and I decide to disable the remember me button

* Change SsrAlgebra to have two phases: prepare and render
* Move SsrImpls into either different package or WSL
* Test MinimalSsr
* Rename SsrMinimal to something with Template in it - maybe "pre-render" too
* Pass in real webserver url instead of https://shipreq.com hardcoded in SsrMinimal
* Restore SsrInterpreter to use SyncTimed (?) -- nah probably just delete
* Add tracing and logging to MinimalSsr prep
* Add proper config for Ssr

* Inspect changes made to html templates (height:100%)
