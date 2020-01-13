# data-aggregator-temp
Temporary code base to perform some data aggregation whilst real service is being built.

This service is meant to be run on the same Azure environment as the storage accounts being used.
Can be run through a container instance but requires manual push of the image to a relevant ACR.

To run this one time, run using the environment variables:
FROM_DAYS_AGO - number of days ago to start from
TO_DAYS_AGO - number of days to go up to (Optional, defaults to today if not populated)

For example, a FROM_DAYS_AGO of 10 and a TO_DAYS_AGO of 3 will aggregate all data from 10 days ago of today's date, up until 3 days ago of today's date.
Note that the date of extraction is one day ahead of the date of data, because daily data is collected the morning after at around 3am (as of current implementation).
That is if you want data from 10 days ago, you only need a FROM_DAYS_AGO of 9. Then today's date will be yesterday's data.

If the application is left running, every Monday at 5am, the weeks data from 7 days ago until today will be collected and stored into the Storage account.