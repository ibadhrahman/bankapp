-- Run this file once in MySQL before starting the program:
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS bankdb;
USE bankdb;

-- Table 1: stores bank accounts (Account Management module)
CREATE TABLE IF NOT EXISTS accounts (
    account_no  INT AUTO_INCREMENT PRIMARY KEY,
    holder_name VARCHAR(50) NOT NULL,
    balance     DOUBLE NOT NULL DEFAULT 0,
    password    VARCHAR(50) NOT NULL DEFAULT '1234'
) AUTO_INCREMENT = 1001;

-- Table 2: stores loans (Loan Management + Repayment modules)
-- status is one of: PENDING, APPROVED, REJECTED, CLOSED
-- Account 1 --- * Loan  (one account can have many loans)
CREATE TABLE IF NOT EXISTS loans (
    loan_id    INT AUTO_INCREMENT PRIMARY KEY,
    account_no INT NOT NULL,
    loan_type  VARCHAR(10) NOT NULL,      -- HOME or PERSONAL
    principal  DOUBLE NOT NULL,
    months     INT NOT NULL,
    remaining  DOUBLE NOT NULL,
    status     VARCHAR(10) NOT NULL,
    FOREIGN KEY (account_no) REFERENCES accounts(account_no)
);
