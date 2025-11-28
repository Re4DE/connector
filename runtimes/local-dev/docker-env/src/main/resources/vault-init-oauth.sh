#!/bin/sh
#
#  Copyright (c) 2025 Fraunhofer Institute for Energy Economics and Energy System Technology (IEE)
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Fraunhofer IEE - initial API and implementation
#
#
set -e

vault login devpass >/dev/null

put_if_missing() {
  path="$1"
  value="$2"

  if ! vault kv get -field=content "$path" >/dev/null 2>&1; then
    vault kv put "$path" content="$value" >/dev/null
  fi
}

put_if_missing secret/auth-cert "$CERT"
put_if_missing secret/auth-key "$PRIV_KEY"
put_if_missing secret/signer-key "$SIGNER_KEY"
put_if_missing secret/verifier-key "$VERIFIER_KEY"
