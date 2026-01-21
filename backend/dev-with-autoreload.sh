#!/bin/bash

# Development mode with auto-reload
# This script starts both continuous build and bootRun in the same terminal
# using tmux for split-pane view

echo "🚀 Starting Backend with Auto-Reload..."
echo ""

# Check if in backend directory
if [ ! -f "build.gradle" ]; then
    echo "❌ Error: Must run from backend directory"
    echo "   cd backend && ./dev-with-autoreload.sh"
    exit 1
fi

# Check if tmux is available
if command -v tmux &> /dev/null; then
    echo "📊 Using tmux for split-pane view"
    echo "   Press Ctrl+B then D to detach"
    echo "   Run 'tmux attach' to reattach"
    echo ""
    
    # Create new tmux session with split panes
    tmux new-session -d -s backend-dev
    
    # Top pane: Continuous build
    tmux send-keys -t backend-dev "echo '📦 Gradle Continuous Build' && ./gradlew build --continuous -x test" C-m
    
    # Split horizontally
    tmux split-window -h -t backend-dev
    
    # Bottom pane: Spring Boot
    tmux send-keys -t backend-dev "echo '🌱 Spring Boot Application' && sleep 3 && ./gradlew bootRun" C-m
    
    # Attach to session
    tmux attach-session -t backend-dev
else
    echo "ℹ️  tmux not found. Starting in background mode..."
    echo ""
    
    # Start continuous build in background
    echo "📦 Starting Gradle continuous build..."
    ./gradlew build --continuous -x test > /tmp/gradle-continuous.log 2>&1 &
    GRADLE_PID=$!
    echo "   PID: $GRADLE_PID"
    echo "   Logs: tail -f /tmp/gradle-continuous.log"
    
    # Wait a bit for initial build
    sleep 3
    
    # Start Spring Boot
    echo ""
    echo "🌱 Starting Spring Boot application..."
    echo "   Press Ctrl+C to stop both processes"
    echo ""
    
    # Cleanup function
    cleanup() {
        echo ""
        echo "🛑 Stopping services..."
        kill $GRADLE_PID 2>/dev/null
        exit 0
    }
    
    trap cleanup INT TERM
    
    # Run Spring Boot in foreground
    ./gradlew bootRun
    
    # Cleanup on exit
    cleanup
fi

