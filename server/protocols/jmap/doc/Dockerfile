FROM python:2.7.11

RUN pip install mkdocs

ADD . /doc

RUN find /doc -type f -name '*.mdwn' -print0 | xargs -0 rename 's/.mdwn$/.md/'

RUN mv /doc/specs/spec/apimodel.md /doc/specs/index.md

WORKDIR /doc

RUN mkdocs build --clean

WORKDIR site

EXPOSE 8000

CMD python -m SimpleHTTPServer 8000