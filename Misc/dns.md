Setup
=====

1. Create DNS zone at host (GCP/AWS)
   Host will provide an NS record.

2. Go to gandi.net, update name servers, give it the URLs in the host NS record.

3. At host, create records in your new zone.

  * `A` record that points to cluster external IP
  * `CNAME` record that points `www` to `shipreq.com.`


Typical Record Types
====================

* `A`     - name → IP4
* `AAAA`  - name → IP6
* `CNAME` - name → name
* `MX`    - email
* `TXT`   - arbitrary, usually for domain verification

