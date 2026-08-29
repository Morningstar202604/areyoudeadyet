#!/bin/bash
# Push to all three code hosting platforms
# Usage: ./push-all.sh [branch]

BRANCH=${1:-main}

echo "🚀 Pushing to GitHub..."
git push origin $BRANCH

echo ""
echo "🚀 Pushing to Gitee..."
git push gitee $BRANCH

echo ""
echo "🚀 Pushing to GitCode..."
git push gitcode $BRANCH

echo ""
echo "✅ All platforms updated!"
