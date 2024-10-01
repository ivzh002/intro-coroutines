package tasks

import contributors.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.coroutineContext

suspend fun loadContributorsSuspend(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .body() ?: emptyList()

    val allUsers = Collections.synchronizedList(mutableListOf<User>())
    val jobs = Collections.synchronizedList(mutableListOf<Job>())
    repos.forEach { repo ->
        jobs.add(
            CoroutineScope(coroutineContext).launch {
                allUsers += service
                    .getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList()
            }
        )
    }

    var isCompleted = false
    while (!isCompleted) {
        delay(250L)
        isCompleted = jobs.all { it.isCompleted }
    }
    return allUsers.aggregate()
}