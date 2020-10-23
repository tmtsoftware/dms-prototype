drop table event_snapshots;
drop index idx_exposure_id_11;

delete from event_snapshots;
select *  from event_snapshots where obseventname='exposureEnd' limit 3;

select *  from event_snapshots where obseventname='exposureEnd' limit 3;
select count(*)  from event_snapshots;

create table event_snapshots
(
    exposureid   varchar(50),
    obseventname varchar(50),
    source       varchar(50),
    eventname    varchar(50),
    eventid      varchar(50),
    eventtime    TIMESTAMP,
    paramset     bytea
);

CREATE INDEX idx_exposure_id_11
    ON event_snapshots (exposureid);
