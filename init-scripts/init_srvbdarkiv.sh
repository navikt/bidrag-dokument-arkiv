#!/bin/bash

export SRV_BD_ARKIV_AUTH="No authentication available"

if test -f /var/run/secrets/dev/srvbdarkiv/password; then
    SRV_BD_ARKIV_AUTH=$(cat /var/run/secrets/dev/srvbdarkiv/password)
    echo Exporting srvbdarkiv authentication for dev cluster
else
  if test -f /var/run/secrets/prod/srvbdarkiv/password; then
    SRV_BD_ARKIV_AUTH=$(cat /var/run/secrets/prod/srvbdarkiv/password)
    echo Exporting srvbdarkiv authentication for prod cluster
  fi
fi
