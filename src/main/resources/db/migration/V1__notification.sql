-- Notification turns other services' events into messages for the user: an in-app inbox, and delivery by push
-- and e-mail. It changes nothing else and publishes no events.

-- Who receives: language (UserProfileUpdated), name, e-mail (taken from the user's token when they enable e-mail)
-- and channel preferences. Created with defaults on first need.
CREATE TABLE recipient (
    user_id       uuid        PRIMARY KEY,
    locale        text        NOT NULL,
    display_name  text        CHECK (char_length(display_name) <= 100),
    email         text        CHECK (char_length(email) <= 320),
    push_enabled  boolean     NOT NULL,
    email_enabled boolean     NOT NULL,
    version       integer     NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    CHECK (NOT email_enabled OR email IS NOT NULL)
);

-- The user's devices that receive push. A token belongs to one user at a time (the last one who registered it).
CREATE TABLE device (
    id           uuid        PRIMARY KEY,
    user_id      uuid        NOT NULL,
    token        text        NOT NULL UNIQUE CHECK (char_length(token) BETWEEN 1 AND 4096),
    platform     text        NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    created_at   timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL
);

CREATE INDEX device_user_idx ON device (user_id);

-- The in-app inbox: one message per source event (a redelivered or repeated event adds nothing).
CREATE TABLE notification (
    id              uuid        PRIMARY KEY,
    user_id         uuid        NOT NULL,
    kind            text        NOT NULL CHECK (kind IN ('DECISION_REVIEW_DUE', 'REOPEN_TRIGGER_FIRED',
        'WEEKLY_REVIEW_DUE', 'DRIFT_DETECTED', 'ASSUMPTION_FALSIFIED', 'PROPOSAL_RECEIVED')),
    source_event_id uuid        NOT NULL UNIQUE,
    -- The decision, weekly plan or proposal it is about, and where the app opens it.
    subject_id      uuid,
    title           text        NOT NULL CHECK (char_length(title) BETWEEN 1 AND 200),
    body            text        NOT NULL CHECK (char_length(body) BETWEEN 1 AND 1000),
    link            text        CHECK (char_length(link) <= 500),
    read_at         timestamptz,
    created_at      timestamptz NOT NULL
);

CREATE INDEX notification_user_idx ON notification (user_id, created_at DESC);

-- Sending a notification on one channel: tried until SENT, retried with growing pauses, FAILED after the last try.
-- Sending is apart from receiving the event, so a slow provider never holds up Kafka.
CREATE TABLE delivery (
    id              uuid        PRIMARY KEY,
    notification_id uuid        NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    user_id         uuid        NOT NULL,
    channel         text        NOT NULL CHECK (channel IN ('PUSH', 'EMAIL')),
    status          text        NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts        integer     NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at timestamptz NOT NULL,
    last_error      text        CHECK (char_length(last_error) <= 1000),
    sent_at         timestamptz,
    version         integer     NOT NULL DEFAULT 0,
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL,
    UNIQUE (notification_id, channel),
    CHECK ((status = 'SENT') = (sent_at IS NOT NULL))
);

-- The sender's question: which deliveries are due?
CREATE INDEX delivery_due_idx ON delivery (next_attempt_at) WHERE status = 'PENDING';
