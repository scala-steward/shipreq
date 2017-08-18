Third-Party Services
====================

## GSuite

Admin - https://admin.google.com/shipreq.com

For signed emails:
1. Add a TXT record for SPF: https://support.google.com/a/answer/178723
2. Enable DKIM
  1. Wait 24-48 hrs (blame Google)
  2. Search for "DKIM" from https://admin.google.com/shipreq.com
  3. …which leads to https://admin.google.com/shipreq.com/AdminHome?fral=1#AppDetails:service=email
  4. Genereate DKIM records
  5. Update DNS

Creating archive@shipreq.com
1. Create new group.
2. Edit group
3. Permissions > Access permissions > Contact the owners of this Group : deselect Public
4. Permissions > Posting permisson > Post : enbable public (so that forwarding rules on incoming emails works)

Creating {news,notice}@shipreq.com
1. Create new group: notice
1. Add alias: news
2. Edit group
3. Permissions > Posting permisson > Post : enbable public
4. Permissions > Posting permisson > Post As The Group : set to All
5. Permissions > Posting permisson > Reply To Author : set to Public

To redirect mail to FreshDesk:
1. Search for "default routing" from https://admin.google.com/shipreq.com
2. …which leads to https://admin.google.com/shipreq.com/AdminHome#AppDetails:service=email&flyout=default_routing
3. Click "ADD SETTING"
  1. Set 'Single recipient' to contact@shipreq.com
  2. Check 'Change envelope recipient' and enter the FreshDesk email
  3. Save.

To BCC FreshDesk ingress to archive@shipreq.com:
1. Edit the route created above.
2. Check 'Add more recipients' > Add > Advanced
3. Uncheck 'Do not deliver spam to this recipient'
3. Uncheck 'Suppress bounces from this recipient'
4. Check 'Change envelope recipient' and enter archive@shipreq.com


## MailChimp

1. Account Settings -> Verified domains -> add `shipreq.com`.
2. Account Settings -> Extras -> Create API key.
3. Lists -> Create
    * Name = Master
    * From Email = contact@shipreq.com
    * From Name = ShipReq
    * Remind-how = You are receiving this email because you opted in at our website.
4. List -> Master -> Settings
  * List name & defaults
    * Turn off: Send a final welcome email
  * List fields and *|MERGE|* tags
    * Name           | text     | y | y | NAME
    * Newsletter     | number   | y | n | NEWSLETTER
    * Account Status | dropdown | y | n | ACCT
        Never, Active
  * Required email footer content <- fill in as appropriate



## FreshDesk

https://shipreq.freshdesk.com

1. Email Settings
    * Global Support Emails --> ShipReq <contact@shipreq.com>
    * BCC --> archive@shipreq.com
2. Email Notifications
    * Agent Notifications > New Ticket Created > Edit
      * Notification: On
      * Subject: [ShipReq Support] New ticket: (#{{ticket.id}}) {{ticket.subject}}
      * Notify Agents > Add
    * Requestor Notifications --> All off
3. Ticket Fields
    * Type --> [x] Required when closing, RFI|RFC|Incident|Problem|Lead|Other
4. Security
    * SSL --> On
