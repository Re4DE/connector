//
//  Copyright (c) 2025 Fraunhofer Institute for Energy Economics and Energy System Technology (IEE)
//
//  This program and the accompanying materials are made available under the
//  terms of the Apache License, Version 2.0 which is available at
//  https://www.apache.org/licenses/LICENSE-2.0
//
//  SPDX-License-Identifier: Apache-2.0
//
//  Contributors:
//       Fraunhofer IEE - initial API and implementation
//
//

user = process.env.MONGO_INITDB_ROOT_USERNAME
password = process.env.MONGO_INITDB_ROOT_PASSWORD
dbName = process.env.MONGO_INITDB_DATABASE

db = db.getSiblingDB(dbName)

// Create user
db.createUser({
    user: user,
    pwd: password,
    roles: [
        { role: 'readWrite', db: dbName }
    ]
});