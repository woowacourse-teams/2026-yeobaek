"""GitHub API calls used by the measurement runner. No application credentials."""
import io
import json
import os
import urllib.error
import urllib.parse
import urllib.request
import zipfile


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class GitHub:
    def __init__(self):
        self.base = "https://api.github.com/repos/" + os.environ["GITHUB_REPOSITORY"]
        self.token = os.environ["GH_TOKEN"]

    def request(self, path, method="GET"):
        request = urllib.request.Request(self.base + path, method=method, headers={
            "Authorization": "Bearer " + self.token,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        })
        with urllib.request.urlopen(request, timeout=10) as response:
            body = response.read(5_000_001)
        if len(body) > 5_000_000:
            raise ValueError("GitHub response too large")
        return json.loads(body) if body else None

    def pages(self, path, key):
        separator = "&" if "?" in path else "?"
        for page in range(1, 101):
            response = self.request(f"{path}{separator}per_page=100&page={page}")
            yield from response[key]
            if len(response[key]) < 100:
                return
        raise ValueError("GitHub pagination limit reached")

    def artifacts(self):
        return list(self.pages("/actions/artifacts", "artifacts"))

    def artifact_json(self, artifact_id, filename):
        request = urllib.request.Request(
            f"{self.base}/actions/artifacts/{int(artifact_id)}/zip",
            headers={"Authorization": "Bearer " + self.token})
        try:
            urllib.request.build_opener(NoRedirect).open(request, timeout=10)
            raise ValueError("Expected GitHub artifact redirect")
        except urllib.error.HTTPError as error:
            if error.code != 302:
                raise
            location = error.headers["Location"]
        if urllib.parse.urlparse(location).scheme != "https":
            raise ValueError("Invalid artifact download URL")
        # Never forward the GitHub token to the artifact storage host.
        with urllib.request.urlopen(location, timeout=10) as response:
            archive = response.read(5_000_001)
        if len(archive) > 5_000_000:
            raise ValueError("Artifact too large")
        with zipfile.ZipFile(io.BytesIO(archive)) as zipped:
            if zipped.getinfo(filename).file_size > 5_000_000:
                raise ValueError("Artifact JSON too large")
            return json.loads(zipped.read(filename))
