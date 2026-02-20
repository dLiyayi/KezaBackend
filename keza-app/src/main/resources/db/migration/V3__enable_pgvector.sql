-- Enable pgvector extension (should already be enabled by init-db.sql, but ensure it exists)
CREATE EXTENSION IF NOT EXISTS vector;

-- AI schema for vector store and AI-related tables
CREATE SCHEMA IF NOT EXISTS ai;
