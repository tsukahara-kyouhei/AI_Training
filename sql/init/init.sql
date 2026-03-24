\echo 'Initialize OFFICE ORDER schema and seed data'
\set ON_ERROR_STOP on

\ir schema/masters.sql
\ir schema/products.sql
\ir schema/members.sql
\ir schema/orders.sql
\ir schema/spring-batch-metadata.sql
\ir schema/content.sql
\ir schema/batch.sql
\ir schema/products_search_idx.sql

\ir seed/masters.sql
\ir seed/products.sql
\ir seed/members.sql
\ir seed/orders.sql
\ir seed/content.sql
