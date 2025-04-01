package com.github.incept5.error

/**
 * Mock implementation of ErrorCode for build purposes
 */
interface ErrorCode {
    val code: String
    val message: String
    val httpStatus: Int
}