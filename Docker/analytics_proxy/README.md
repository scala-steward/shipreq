# Setup

```sh
git remote add analytics https://github.com/ZitRos/save-analytics-from-content-blockers.git
git fetch analytics
git checkout a5c5320
git checkout -b fork/analytics
```

# Copying custom changes to fork source

```sh
./copy
cd $sq
git checkout fork/analytics
unzip /tmp/analytics_proxy-Xxxxxxxxxx
# Inspect changes
git add -u
git commit -m 'Integrate my custom changes'
git checkout -
cd Docker/analytics_proxy
```

# Merging upstream fork source

```sh
cd $sq
git checkout fork/analytics
git fetch analytics
git merge analytics/master
./copy
git checkout -
cd $sq/Docker/analytics_proxy
rm -rf src && unzip -o /tmp/analytics_proxy-Xxxxxxxxxx
# Inspect changes
git add .
git commit
```

# Testing

1. Run `make test` or `make run`
2. Open up test-static/index.html in a browser
3. In the browser tab, click the AdBlock button, then click Open Logger (the 3rd button under the power button)
4. Reload and look at the network tab, ensure all requests succeed
   If you see `net::ERR_BLOCKED_BY_CLIENT` errors, that means AdBlock is blocking requests.
