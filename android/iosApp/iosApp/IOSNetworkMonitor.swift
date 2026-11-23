import Foundation
import Network
import Shared

final class IOSNetworkMonitor {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.yeobaek.network-monitor")
    private let lock = NSLock()
    private var started = false
    private var closed = false

    lazy var state = MutableNetworkMonitor(onClose: { [weak self] in
        self?.close()
    })

    func start() {
        lock.lock()
        defer { lock.unlock() }
        guard !started && !closed else { return }
        started = true
        _ = state
        monitor.pathUpdateHandler = { [weak self] path in
            self?.updateConnected(path.status == .satisfied)
        }
        monitor.start(queue: queue)
    }

    private func updateConnected(_ connected: Bool) {
        lock.lock()
        defer { lock.unlock() }
        guard !closed else { return }
        state.updateConnected(connected: connected)
    }

    func close() {
        lock.lock()
        defer { lock.unlock() }
        guard !closed else { return }
        closed = true
        monitor.cancel()
        state.updateConnected(connected: false)
    }

    deinit {
        monitor.cancel()
    }
}
