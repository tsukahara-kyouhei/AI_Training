CREATE TABLE IF NOT EXISTS batch_job_instance (
    job_instance_id BIGINT PRIMARY KEY,
    version BIGINT,
    job_name VARCHAR(100) NOT NULL,
    job_key VARCHAR(32) NOT NULL,
    CONSTRAINT uq_batch_job_instance UNIQUE (job_name, job_key)
);

CREATE TABLE IF NOT EXISTS batch_job_execution (
    job_execution_id BIGINT PRIMARY KEY,
    version BIGINT,
    job_instance_id BIGINT NOT NULL,
    create_time TIMESTAMP(9) NOT NULL,
    start_time TIMESTAMP(9),
    end_time TIMESTAMP(9),
    status VARCHAR(10),
    exit_code VARCHAR(2500),
    exit_message VARCHAR(2500),
    last_updated TIMESTAMP(9),
    CONSTRAINT fk_batch_job_execution_instance
        FOREIGN KEY (job_instance_id) REFERENCES batch_job_instance (job_instance_id)
);

CREATE TABLE IF NOT EXISTS batch_job_execution_params (
    job_execution_id BIGINT NOT NULL,
    parameter_name VARCHAR(100) NOT NULL,
    parameter_type VARCHAR(100) NOT NULL,
    parameter_value VARCHAR(2500),
    identifying CHAR(1) NOT NULL,
    CONSTRAINT fk_batch_job_execution_params_execution
        FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution (job_execution_id)
);

CREATE TABLE IF NOT EXISTS batch_step_execution (
    step_execution_id BIGINT PRIMARY KEY,
    version BIGINT NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    job_execution_id BIGINT NOT NULL,
    create_time TIMESTAMP(9) NOT NULL,
    start_time TIMESTAMP(9),
    end_time TIMESTAMP(9),
    status VARCHAR(10),
    commit_count BIGINT,
    read_count BIGINT,
    filter_count BIGINT,
    write_count BIGINT,
    read_skip_count BIGINT,
    write_skip_count BIGINT,
    process_skip_count BIGINT,
    rollback_count BIGINT,
    exit_code VARCHAR(2500),
    exit_message VARCHAR(2500),
    last_updated TIMESTAMP(9),
    CONSTRAINT fk_batch_step_execution_job
        FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution (job_execution_id)
);

CREATE TABLE IF NOT EXISTS batch_step_execution_context (
    step_execution_id BIGINT PRIMARY KEY,
    short_context VARCHAR(2500) NOT NULL,
    serialized_context TEXT,
    CONSTRAINT fk_batch_step_execution_context_step
        FOREIGN KEY (step_execution_id) REFERENCES batch_step_execution (step_execution_id)
);

CREATE TABLE IF NOT EXISTS batch_job_execution_context (
    job_execution_id BIGINT PRIMARY KEY,
    short_context VARCHAR(2500) NOT NULL,
    serialized_context TEXT,
    CONSTRAINT fk_batch_job_execution_context_execution
        FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution (job_execution_id)
);

CREATE INDEX IF NOT EXISTS idx_batch_job_execution_instance
    ON batch_job_execution (job_instance_id);
CREATE INDEX IF NOT EXISTS idx_batch_step_execution_job
    ON batch_step_execution (job_execution_id);

CREATE SEQUENCE IF NOT EXISTS batch_step_execution_seq
    MAXVALUE 9223372036854775807
    NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS batch_job_execution_seq
    MAXVALUE 9223372036854775807
    NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS batch_job_seq
    MAXVALUE 9223372036854775807
    NO CYCLE;
