#!/bin/bash

export SRV_BD_ARKIV_AUTH="No authentication available"

PATH_PASSWORD=/var/run/secrets/nais.io/srvbdarkiv/password

if test -f "$PATH_PASSWORD"; then
  SRV_BD_ARKIV_AUTH=$(cat "$PATH_PASSWORD")
  echo Exporting srvbdarkiv authentication
else
  echo No authentication for srvbdarkiv is exported...
fi
