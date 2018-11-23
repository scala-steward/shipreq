* prometheus buckets
* ReactDOM needs hydrate
* abort if not completed within X
* docker base image with graal
* replace webapp-gen with ssr (means it becomes mandatory instead of optional (?) or maybe some crap default)


* Router in SSR tries to pushState/replaceState
  - let it crash and hide from logs?
  - or, have the router be SSR aware and....?

* need to render after hydration...
  - if window.location.hash is set (because server doesn't consider)
  - if localStorage isn't available and I decide to disable the remember me button
