var minDistance = null;
var lastMinNode = null;

var countTextBlocks = 0
var bigTextIndex = null

function createBigTextIndex(thetext) {
    try {
        var childNodes = thetext.childNodes;
        var range = document.createRange();

        for (var ch = 0; ch < childNodes.length; ch++) {
            var node = childNodes[ch];

            if (node.nodeType == 3 /*Text*/) {
                countTextBlocks++
                var textValue = node.nodeValue;

                textValue = $.trim(textValue)

                if (textValue.length == 0)
                    continue;

                range.setStart(node, 0)
                range.setEnd(node, textValue.length)

                var boundingRect = range.getBoundingClientRect();

                node.top = boundingRect.top
                node.bottom = boundingRect.bottom
                node.left = boundingRect.left
                node.right = boundingRect.right

                bigTextIndex.push(node)
            }
            else if (node.nodeType == 1 /*Element*/) {
                createBigTextIndex(node);
            }
        }
    }
    catch(e) {
        console.log(e)
        //Bummer
    }
}

function findClosestTextBlock(thetext, event, maxDistance) {
    if (!maxDistance)
        maxDistance = 15

    var x = event.clientX;
    var y = event.clientY;
    var sourcePoint = {x:x, y:y};

    try {
        for (var ch = 0; ch < bigTextIndex.length; ch++) {
            var node = bigTextIndex[ch];

            var inX = x >= node.left && x <= node.right
            var inY = y >= node.top && y <= node.bottom

            var minDiff = null

            if (inX) {
                if (inY) {
                    return node;
                }

                minDiff = Math.min(Math.abs(y - node.top), Math.abs(y - node.bottom))
                if (minDiff > maxDistance)
                    minDiff = null
            }

            var yDiff = 10000

            if (inY) {
                minDiff = Math.min(Math.abs(x - node.left), Math.abs(x - node.right))
                yDiff = minDiff
            }

            if (minDiff != null && (minDistance == null || minDistance >= minDiff)) {
                minDistance = minDiff
                lastMinNode = node
            }
        }
    }
    catch(e) {
        console.log(e)
        //Bummer
    }

    return null
}

var nodeIndex = new Object()
var nodeId = 0

function loadNodeIndex(node) {
    if (node.textIndex) {
        var textPosIndex = nodeIndex[node.textIndex];
        if (textPosIndex)
            return textPosIndex
    }

    var textValue = node.nodeValue;
    var startChar = null
    var rangeList = []

    for (var i = 0; i < textValue.length; i++) {
        var testChar = textValue.charAt(i);
        if (
            (testChar >= 'a' && testChar <= 'z' ) ||
                (testChar >= 'A' && testChar <= 'Z' ) ||
                (testChar >= '0' && testChar <= '9' ) ||
                testChar == '-' ||
                testChar == '_' ||
                testChar == '.' ||
                testChar == ':') {
            if (startChar == null)
                startChar = i
        }
        else {
            if (startChar != null) {
                rangeList.push(makeWordRange(node, startChar, i))
                startChar = null
            }
        }
    }

    if (startChar != null) {
        rangeList.push(makeWordRange(node, startChar, node.nodeValue.length))
        startChar = null
    }

    nodeId++
    node.textIndex = "a_" + nodeId
    nodeIndex[node.textIndex] = rangeList

    return rangeList
}

function makeWordRange(node, startChar, i) {
    var range = document.createRange();
    range.setStart(node, startChar)
    range.setEnd(node, i)
    var boundingRect = range.getBoundingClientRect();
    range.y = (boundingRect.bottom + boundingRect.top) / 2
    range.x = (boundingRect.right + boundingRect.left) / 2
    range.left = boundingRect.left
    range.right = boundingRect.right

    return range;
}

function findClosestRange(node, event) {
    var rangeList = loadNodeIndex(node);
    var x = event.clientX;
    var y = event.clientY;

    var minDistance = 10000000
    var minRange = null

    for (var i = 0; i < rangeList.length; i++) {
        var range = rangeList[i];

        var theDistance = lineDistance({x:x, y:y}, range);

        if (theDistance < minDistance) {
            minDistance = theDistance
            minRange = range
        }
    }

    return minRange
}

function lineDistance(point1, point2) {
    var xs = 0;
    var ys = 0;

    xs = point2.x - point1.x;
    xs = xs * xs;

    ys = point2.y - point1.y;
    ys = ys * ys;

    return Math.sqrt(xs + ys);
}

var selecting = false
var startSelectingEvent = null

var selectionStart = null
var selectionEnd = null

/*var pointDownFunc = function(event)
 {
 if(selecting)
 {
 checkForPosition(event)
 selecting = false
 }
 };*/

function startSelection() {
    clearDecks(true)
}

function cancelSelection() {
    clearDecks(false)
    hideHandles()
}

function clearDecks(sel) {
    console.log("clearDecks(" + sel + ")")

    selectionStart = null
    selectionEnd = null
    bigTextIndex = []
    nodeIndex = new Object()

    window.getSelection().removeAllRanges()

    selecting = sel;

    if (selecting) {
        createBigTextIndex(document.getElementById("wrap"))
        if (startSelectingEvent != null)
            selectClosestWord(startSelectingEvent)
//            checkForPosition(startSelectingEvent)
    }
}

document.ontouchstart = function(e) {
    if (selecting)
        e.preventDefault()

    var event = e.touches[0];
    /*var x = event.clientX;
     var y = event.clientY;

     if(selectionStart != null && selectionEnd != null)
     {
     var startDist = lineDistance({x:x, y:y}, {x:selectionStart.x,  y:selectionStart.y});
     var endDist = lineDistance({x:x, y:y}, {x:selectionEnd.x,  y:selectionEnd.y});

     if(startDist < endDist)
     {
     var temp = selectionStart
     selectionStart = selectionEnd
     selectionEnd = temp
     }
     }*/

    selectionStart = null
    selectionEnd = null

    console.log("ontouchstart: " + e + "/selectin: " + selecting)

    startSelectingEvent = event

    checkForPosition(event)
}

/*document.ontouchend = function(event)
 {
 console.log("ontouchend")
 pointDownFunc(event.touches[0])
 }*/

document.ontouchmove = function(e) {
    if (selecting)
        e.preventDefault()
    console.log("ontouchmove")
    checkForPosition(e.touches[0])
}

document.onmousemove = function (event) {
    console.log("onmousemove (ignore)")
    paintSelection()
}

function selectClosestWord(event) {
    var thetext = document.getElementById("wrap");

    var node = findClosestTextBlock(thetext, event, 100)
    if (node == null && lastMinNode != null && minDistance < 140)
        node = lastMinNode

    if (node != null) {
        var range = findClosestRange(node, event)

        if (range != null) {
            selectionStart = range
            selectionEnd = range
            positionHandles()
            htmlSelectActivity.pushAutoSelect();
        }
    }
}

function generateCombinedRange() {
    if (selecting && selectionStart && selectionEnd) {
        var showStart, showEnd

        if (selectionStart.compareBoundaryPoints(1, selectionEnd) > 0) {
            showEnd = selectionStart
            showStart = selectionEnd
        }
        else {
            showStart = selectionStart
            showEnd = selectionEnd
        }

        var combinedRange = document.createRange();
        combinedRange.setStart(showStart.startContainer, showStart.startOffset)
        combinedRange.setEnd(showEnd.endContainer, showEnd.endOffset)

        return combinedRange
    }

    return null
}

function paintSelection() {
    var combinedRange = generateCombinedRange()
    if (combinedRange != null) {
        window.getSelection().removeAllRanges()
        window.getSelection().addRange(combinedRange)

        positionHandles()
    }
}
function checkForPosition(event) {
    console.log("checkForPosition " + event)

    if (selecting && event) {
        var thetext = document.getElementById("wrap");

        minDistance = null
        lastMinNode = null

        countTextBlocks = 0
        var node = findClosestTextBlock(thetext, event)
        if (node == null && lastMinNode != null && minDistance < 140)
            node = lastMinNode

        if (node != null) {
            /*var combinedRange = document.createRange();
             combinedRange.setStart(node, 0)
             combinedRange.setEnd(node, node.nodeValue.length)

             window.getSelection().removeAllRanges()
             window.getSelection().addRange(combinedRange)*/

            var range = findClosestRange(node, event)

            if (range != null) {
                if (selectionStart == null) {
                    selectionStart = range
                    selectionEnd = range
                }
                else {
                    selectionEnd = range
                }

                paintSelection();
            }
        }
    }
}

var SEL_START_HANDLE = "selStartHandle"
var SEL_END_HANDLE = "selEndHandle"

function cancelBubble(e) {
    var evt = e ? e : window.event;
    if (evt.stopPropagation)    evt.stopPropagation();
    if (evt.cancelBubble != null) evt.cancelBubble = true;
}

function areSelectionsBackward(start, end) {
    return start.compareBoundaryPoints(1, end) > 0;
}

function areSelectionsInverted() {
    return areSelectionsBackward(selectionStart, selectionEnd);
}

function positionHandles() {
    var selStart = document.getElementById(SEL_START_HANDLE);
    var selEnd = document.getElementById(SEL_END_HANDLE);

    if (!selStart) {
        var addHandle = function(imgId, imgName, isStart) {
            var selHand = document.createElement("img")
            selHand.id = imgId
            selHand.src = "file:///android_asset/img/" + imgName + ".png"
            selHand.style.display = "none"
            selHand.style.position = "fixed"

            selHand.ontouchstart = function(e) {
                var wasInverted = false;

                //Yeah, closures!
                if (
                    (!isStart && areSelectionsInverted())
                        ||
                        (isStart && !areSelectionsInverted())
                    ) {
                    var temp = selectionStart
                    selectionStart = selectionEnd
                    selectionEnd = temp
                    wasInverted = true
                }

                console.log("ontouchstart - isStart: " + isStart + "/wasInverted: " + wasInverted)

                cancelBubble(e)
            }

            document.body.appendChild(selHand)
            return selHand;
        }

        selStart = addHandle(SEL_START_HANDLE, "selhand_start", true);
        selEnd = addHandle(SEL_END_HANDLE, "selhand_end", false);
    }

//    selStart = $(selStart)
//    selStart = $(selEnd)

    var showStart, showEnd

    if (areSelectionsInverted()) {
        showEnd = selectionStart
        showStart = selectionEnd
    }
    else {
        showStart = selectionStart
        showEnd = selectionEnd
    }

    selStart.style.display = ""
    selEnd.style.display = ""
    selStart.style.left = (showStart.left - 25) + "px"
    selStart.style.top = (showStart.y - 15) + "px"
    selEnd.style.left = (showEnd.right - 1) + "px"
    selEnd.style.top = (showEnd.y - 15) + "px"
}

function hideHandles() {
    var selStart = document.getElementById(SEL_START_HANDLE);
    var selEnd = document.getElementById(SEL_END_HANDLE);

    if (selStart)
        selStart.style.display = "none"
    if (selEnd)
        selEnd.style.display = "none"
}

//The mouseup is clearing selections, which is really frustrating
$(document).ready(function() {
    $(window).mouseup(
        function() {
            setTimeout(paintSelection, 75)
        }
    );
})


function reportSelectionCoords() {
    try {
        var combinedRange = generateCombinedRange()
        if (combinedRange != null) {
            var bounds = combinedRange.getBoundingClientRect();
            var midX = (bounds.right + bounds.left) / 2

            if (midX > 0)
                htmlSelectActivity.pushSelectionCoords(midX, bounds.top)
        }
    }
    catch(e) {
        console.log(e)
    }
}


function OSgetSelection() {

    var s = window.getSelection();
    var range = s.getRangeAt(0);
    console.log("!@#$%" + range);

    htmlSelectActivity.pushSelection(range.toString());
}






