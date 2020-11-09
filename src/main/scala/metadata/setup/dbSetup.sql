drop table event_snapshots;
drop index idx_exposure_id;

delete from event_snapshots;
select *  from event_snapshots where obs_event_name='exposureEnd' limit 3;

select *  from event_snapshots where obs_event_name='exposureEnd' limit 3;
select count(*)  from event_snapshots;


/* create snapshots table and index */
create table event_snapshots
(
    exposure_id    varchar(50) not null,
    obs_event_name varchar(50) not null,
    source         varchar(50) not null,
    eventname      varchar(50) not null,
    eventid        varchar(50) not null,
    eventtime      timestamp   not null,
    paramset       text
);

create index idx_exposure_id
    on event_snapshots (exposure_id);


/* create Keywords table and index */

create table keyword_values
(
    exposure_id varchar(50) not null,
    keyword     varchar(50) not null,
    value       varchar(50) not null
);

create index idx_hdr_exposure_id
    on keyword_values (exposure_id);




alter table event_snapshots
    owner to postgres;


alter table keyword_values
    owner to postgres;
