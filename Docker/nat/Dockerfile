FROM sameersbn/squid:3.5.27-2

COPY ./squid.conf /etc/squid/squid.conf

EXPOSE 3128 3129 3130

COPY ./domain-whitelist.txt /etc/squid/domain-whitelist.txt
