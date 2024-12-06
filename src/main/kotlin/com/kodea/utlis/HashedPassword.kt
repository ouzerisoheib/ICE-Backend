package com.kodea.utlis

import org.mindrot.jbcrypt.BCrypt

fun hashedPassword(password: String) = BCrypt.hashpw(password, BCrypt.gensalt(10))