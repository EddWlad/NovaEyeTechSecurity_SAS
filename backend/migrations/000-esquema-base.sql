--
-- PostgreSQL database dump
--

\restrict w2ltayHh6AjMl1BrQEanJNfDSbKmAIgLUog1y0ABV1abHNWv5snOBg38jXAJgS5

-- Dumped from database version 16.13 (Debian 16.13-1.pgdg13+1)
-- Dumped by pg_dump version 16.13 (Debian 16.13-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: maintenance_status_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.maintenance_status_enum AS ENUM (
    'PENDIENTE',
    'EN_PROCESO',
    'COMPLETADO',
    'CANCELADO'
);


--
-- Name: maintenance_type_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.maintenance_type_enum AS ENUM (
    'PREVENTIVO',
    'CORRECTIVO'
);


--
-- Name: quotation_details_itemtype_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.quotation_details_itemtype_enum AS ENUM (
    'PRODUCTO',
    'SERVICIO'
);


--
-- Name: quotations_status_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.quotations_status_enum AS ENUM (
    'BORRADOR',
    'ENVIADA',
    'APROBADA',
    'RECHAZADA'
);


--
-- Name: users_role_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.users_role_enum AS ENUM (
    'ADMINISTRADOR',
    'TECNICO'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attachments (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "sourceEntity" character varying(80) NOT NULL,
    "sourceEntityId" character varying(80) NOT NULL,
    "originalName" character varying(255) NOT NULL,
    "storedName" character varying(255) NOT NULL,
    "mimeType" character varying(120) NOT NULL,
    "storagePath" character varying(255) NOT NULL,
    size bigint NOT NULL,
    "uploadedBy" character varying(180) NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    module character varying(80) NOT NULL,
    entity character varying(80) NOT NULL,
    "entityId" character varying(80) NOT NULL,
    action character varying(30) NOT NULL,
    "user" character varying(180) NOT NULL,
    summary character varying(300) NOT NULL,
    "payloadSummary" text,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: clients; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clients (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "nameOrBusinessName" character varying(180) NOT NULL,
    "documentNumber" character varying(30) NOT NULL,
    phone character varying(30) NOT NULL,
    email character varying(180),
    address character varying(255) NOT NULL,
    city character varying(120) NOT NULL,
    "commercialReference" character varying(255),
    observations text,
    active boolean DEFAULT true NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: maintenance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.maintenance (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    type public.maintenance_type_enum NOT NULL,
    status public.maintenance_status_enum DEFAULT 'PENDIENTE'::public.maintenance_status_enum NOT NULL,
    "scheduledDate" date NOT NULL,
    "executionDate" date,
    "intervenedSystem" character varying(255) NOT NULL,
    diagnosis text NOT NULL,
    "appliedSolution" text NOT NULL,
    observations text,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL,
    client_id uuid NOT NULL,
    technician_id uuid NOT NULL
);


--
-- Name: maintenance_comments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.maintenance_comments (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    comment text NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    maintenance_id uuid NOT NULL,
    user_id uuid NOT NULL
);


--
-- Name: product_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_categories (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(120) NOT NULL,
    description text,
    active boolean DEFAULT true NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "internalCode" character varying(80) NOT NULL,
    name character varying(180) NOT NULL,
    brand character varying(120) NOT NULL,
    model character varying(120),
    description text NOT NULL,
    "baseCost" numeric(12,2) NOT NULL,
    stock numeric(12,2),
    unit character varying(30) NOT NULL,
    "imageUrl" text,
    active boolean DEFAULT true NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL,
    category_id uuid NOT NULL,
    main_supplier_id uuid
);


--
-- Name: quotation_details; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quotation_details (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "itemType" public.quotation_details_itemtype_enum NOT NULL,
    "referenceId" character varying(80) NOT NULL,
    "descriptionFrozen" character varying(255) NOT NULL,
    quantity numeric(12,2) NOT NULL,
    "basePriceHistorical" numeric(14,2) NOT NULL,
    "vatPercentHistorical" numeric(5,2) NOT NULL,
    "marginPercentHistorical" numeric(5,2) NOT NULL,
    "unitPriceFinal" numeric(14,2) NOT NULL,
    "lineSubtotalBase" numeric(14,2) NOT NULL,
    "lineVatValue" numeric(14,2) NOT NULL,
    "lineTotal" numeric(14,2) NOT NULL,
    quotation_id uuid
);


--
-- Name: quotation_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quotation_settings (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "currentVat" numeric(5,2) DEFAULT 15.00 NOT NULL,
    "allowedVatRates" text DEFAULT '0,1,12,15'::text NOT NULL,
    "allowedMargins" text DEFAULT '0,10,12,20,25,30'::text NOT NULL,
    "defaultMargin" numeric(5,2) DEFAULT 20.00 NOT NULL,
    "defaultCurrency" character varying(8) DEFAULT 'USD'::character varying NOT NULL,
    "defaultValidityDays" integer DEFAULT 15 NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: quotations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quotations (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "quotationNumber" character varying(40) NOT NULL,
    "issuedAt" date NOT NULL,
    "validUntil" date NOT NULL,
    status public.quotations_status_enum DEFAULT 'BORRADOR'::public.quotations_status_enum NOT NULL,
    observations text,
    subtotal numeric(14,2) NOT NULL,
    discount numeric(14,2) DEFAULT 0.00 NOT NULL,
    "vatPercentHistorical" numeric(5,2) NOT NULL,
    "vatValueHistorical" numeric(14,2) NOT NULL,
    total numeric(14,2) NOT NULL,
    currency character varying(8) DEFAULT 'USD'::character varying NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL,
    client_id uuid NOT NULL,
    created_by_user_id uuid NOT NULL
);


--
-- Name: service_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_categories (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(120) NOT NULL,
    description text,
    active boolean DEFAULT true NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: services; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.services (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(180) NOT NULL,
    description text NOT NULL,
    "baseCost" numeric(12,2) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL,
    category_id uuid NOT NULL
);


--
-- Name: suppliers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suppliers (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "businessName" character varying(180) NOT NULL,
    ruc character varying(30) NOT NULL,
    contact character varying(180) NOT NULL,
    phone character varying(30) NOT NULL,
    email character varying(180),
    address character varying(255) NOT NULL,
    city character varying(120) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    email character varying(180) NOT NULL,
    "fullName" character varying(180) NOT NULL,
    password character varying(255) NOT NULL,
    role public.users_role_enum DEFAULT 'TECNICO'::public.users_role_enum NOT NULL,
    phone character varying(30),
    "avatarDataUrl" text,
    active boolean DEFAULT true NOT NULL,
    "createdAt" timestamp without time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: products PK_0806c755e0aca124e67c0cf6d7d; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT "PK_0806c755e0aca124e67c0cf6d7d" PRIMARY KEY (id);


--
-- Name: audit_logs PK_1bb179d048bbc581caa3b013439; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT "PK_1bb179d048bbc581caa3b013439" PRIMARY KEY (id);


--
-- Name: maintenance PK_542fb6a28537140d2df95faa52a; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance
    ADD CONSTRAINT "PK_542fb6a28537140d2df95faa52a" PRIMARY KEY (id);


--
-- Name: attachments PK_5e1f050bcff31e3084a1d662412; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachments
    ADD CONSTRAINT "PK_5e1f050bcff31e3084a1d662412" PRIMARY KEY (id);


--
-- Name: quotations PK_6c00eb8ba181f28c21ffba7ecb1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quotations
    ADD CONSTRAINT "PK_6c00eb8ba181f28c21ffba7ecb1" PRIMARY KEY (id);


--
-- Name: product_categories PK_7069dac60d88408eca56fdc9e0c; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_categories
    ADD CONSTRAINT "PK_7069dac60d88408eca56fdc9e0c" PRIMARY KEY (id);


--
-- Name: maintenance_comments PK_7f982ba003e023fa790b3771a9f; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_comments
    ADD CONSTRAINT "PK_7f982ba003e023fa790b3771a9f" PRIMARY KEY (id);


--
-- Name: users PK_a3ffb1c0c8416b9fc6f907b7433; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT "PK_a3ffb1c0c8416b9fc6f907b7433" PRIMARY KEY (id);


--
-- Name: suppliers PK_b70ac51766a9e3144f778cfe81e; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT "PK_b70ac51766a9e3144f778cfe81e" PRIMARY KEY (id);


--
-- Name: services PK_ba2d347a3168a296416c6c5ccb2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT "PK_ba2d347a3168a296416c6c5ccb2" PRIMARY KEY (id);


--
-- Name: quotation_settings PK_bffc53496200618023d684e9df3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quotation_settings
    ADD CONSTRAINT "PK_bffc53496200618023d684e9df3" PRIMARY KEY (id);


--
-- Name: clients PK_f1ab7cf3a5714dbc6bb4e1c28a4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT "PK_f1ab7cf3a5714dbc6bb4e1c28a4" PRIMARY KEY (id);


--
-- Name: quotation_details PK_f5faae63247660701bce1dd4106; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quotation_details
    ADD CONSTRAINT "PK_f5faae63247660701bce1dd4106" PRIMARY KEY (id);


--
-- Name: service_categories PK_fe4da5476c4ffe5aa2d3524ae68; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_categories
    ADD CONSTRAINT "PK_fe4da5476c4ffe5aa2d3524ae68" PRIMARY KEY (id);


--
-- Name: quotations UQ_1abd99974f3059c04df9104a764; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quotations
    ADD CONSTRAINT "UQ_1abd99974f3059c04df9104a764" UNIQUE ("quotationNumber");


--
-- Name: products UQ_20acb64ccff330376f2e626197a; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT "UQ_20acb64ccff330376f2e626197a" UNIQUE ("internalCode");


--
-- Name: service_categories UQ_7ef2e28b495d09a4eb28997c653; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_categories
    ADD CONSTRAINT "UQ_7ef2e28b495d09a4eb28997c653" UNIQUE (name);


--
-- Name: users UQ_97672ac88f789774dd47f7c8be3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT "UQ_97672ac88f789774dd47f7c8be3" UNIQUE (email);


--
-- Name: product_categories UQ_a75bfadcd8291a0538ab7abfdcf; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_categories
    ADD CONSTRAINT "UQ_a75bfadcd8291a0538ab7abfdcf" UNIQUE (name);


--
-- Name: suppliers UQ_b3fa1dc44f770c830541c4223dd; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT "UQ_b3fa1dc44f770c830541c4223dd" UNIQUE (ruc);


--
-- Name: clients UQ_d866e63d1c138ea2de12f4676ec; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT "UQ_d866e63d1c138ea2de12f4676ec" UNIQUE ("documentNumber");


--
-- Name: quotations FK_118e5246cab853e3c1d958732d8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quotations
    ADD CONSTRAINT "FK_118e5246cab853e3c1d958732d8" FOREIGN KEY (client_id) REFERENCES public.clients(id);


--
-- Name: services FK_1f8d1173481678a035b4a81a4ec; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT "FK_1f8d1173481678a035b4a81a4ec" FOREIGN KEY (category_id) REFERENCES public.service_categories(id);


--
-- Name: quotation_details FK_3b546c5d73429058bfc67dd9961; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quotation_details
    ADD CONSTRAINT "FK_3b546c5d73429058bfc67dd9961" FOREIGN KEY (quotation_id) REFERENCES public.quotations(id) ON DELETE CASCADE;


--
-- Name: products FK_8aaf63c60a2abb0a2b4e86eca75; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT "FK_8aaf63c60a2abb0a2b4e86eca75" FOREIGN KEY (main_supplier_id) REFERENCES public.suppliers(id);


--
-- Name: products FK_9a5f6868c96e0069e699f33e124; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT "FK_9a5f6868c96e0069e699f33e124" FOREIGN KEY (category_id) REFERENCES public.product_categories(id);


--
-- Name: maintenance FK_c1750fadc48778d1bc76ea6fa56; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance
    ADD CONSTRAINT "FK_c1750fadc48778d1bc76ea6fa56" FOREIGN KEY (technician_id) REFERENCES public.users(id);


--
-- Name: maintenance_comments FK_c6d607ee6d4fd69e0a41a5698b2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_comments
    ADD CONSTRAINT "FK_c6d607ee6d4fd69e0a41a5698b2" FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: quotations FK_d254de24581fc5fce8955c89651; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quotations
    ADD CONSTRAINT "FK_d254de24581fc5fce8955c89651" FOREIGN KEY (created_by_user_id) REFERENCES public.users(id);


--
-- Name: maintenance FK_e0fd1f50bc851913c737dc44cb4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance
    ADD CONSTRAINT "FK_e0fd1f50bc851913c737dc44cb4" FOREIGN KEY (client_id) REFERENCES public.clients(id);


--
-- Name: maintenance_comments FK_e502e3e554fd2096ea2f32b8bbe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_comments
    ADD CONSTRAINT "FK_e502e3e554fd2096ea2f32b8bbe" FOREIGN KEY (maintenance_id) REFERENCES public.maintenance(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict w2ltayHh6AjMl1BrQEanJNfDSbKmAIgLUog1y0ABV1abHNWv5snOBg38jXAJgS5

