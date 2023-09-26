/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.sql.config

/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */


import com.google.gson.annotations.SerializedName

enum class DbServerType(val driverClassName: String, val connectString: String) {

    @SerializedName("postgresql")
    POSTGRES("org.postgresql.Driver", "jdbc:postgresql://\$host:\$port/\$dbName"),

    @SerializedName("mariadb")
    MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb://\$host:\$port/\$dbName"),

    @SerializedName("sqlserver")
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://\$host:\$port;database=\$dbName;encrypt=true;trustServerCertificate=true;"),

    @SerializedName("mysql")
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://\$host:\$port/\$dbName"),

    @SerializedName("oracle")
    ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@\$host:\$port:\$dbName");

    override fun toString(): String {
        return when (this) {
            POSTGRES -> "postgresql"
            MARIADB -> "mariadb"
            SQLSERVER -> "sqlserver"
            ORACLE -> "oracle"
            MYSQL -> "mysql"
        }
    }

}

