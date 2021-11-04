-- Table: public.migration_version

-- DROP TABLE public.migration_version;

CREATE TABLE IF NOT EXISTS public.migration_version
(
    "version" integer NOT NULL,
    "date" timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT "migration_version_pkey" PRIMARY KEY ("version")
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;