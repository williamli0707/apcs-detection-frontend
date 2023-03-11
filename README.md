# APCS Detection Frontend

This is a full stack application designed to detect potential cheating in Mark Kwong's intro to cs Runestone submissions.
To submit scan requests, use [runestone-submission-downloader](https://github.com/caupcakes/runestone-submission-downloader) and update [line 17](https://github.com/caupcakes/runestone-submission-downloader/blob/388921f2cc37dd37d9c89449bc1e59511b5e8450/src/background.js#L17) with the service url

### Setup

All values in `.env` must be configured.

Here are some links to the relevant services \
[Mailtrap](https://mailtrap.io/) \
[Mongodb](https://www.mongodb.com/atlas)

### Building

Use `docker build -t apcs-detection-frontend .` to build the project. If any errors occur in compiling the project, this will fail

### Running

Use `docker run --env-file .env apcs-detection-frontend` to run the project.


### Dependencies

Do not use dependabot to upgrade any Vaadin or Spring related dependencies.

The first 5 dependencies defined in `pom.xml` are manually configured, and safe to update, while the rest of the `pom.xml` file is generated from [QuickStart](https://vaadin.com/docs/latest/guide/quick-start)
