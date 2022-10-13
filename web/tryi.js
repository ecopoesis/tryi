const Base64Binary = {
    _keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    /* will return a  Uint8Array type */
    decodeArrayBuffer: function(input) {
        var bytes = (input.length/4) * 3;
        var ab = new ArrayBuffer(bytes);
        this.decode(input, ab);

        return ab;
    },

    removePaddingChars: function(input){
        var lkey = this._keyStr.indexOf(input.charAt(input.length - 1));
        if(lkey == 64){
            return input.substring(0,input.length - 1);
        }
        return input;
    },

    decode: function (input, arrayBuffer) {
        //get last chars to see if are valid
        input = this.removePaddingChars(input);
        input = this.removePaddingChars(input);

        var bytes = parseInt((input.length / 4) * 3, 10);

        var uarray;
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;
        var j = 0;

        if (arrayBuffer)
            uarray = new Uint8Array(arrayBuffer);
        else
            uarray = new Uint8Array(bytes);

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        for (i=0; i<bytes; i+=3) {
            //get the 3 octects in 4 ascii chars
            enc1 = this._keyStr.indexOf(input.charAt(j++));
            enc2 = this._keyStr.indexOf(input.charAt(j++));
            enc3 = this._keyStr.indexOf(input.charAt(j++));
            enc4 = this._keyStr.indexOf(input.charAt(j++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            uarray[i] = chr1;
            if (enc3 != 64) uarray[i+1] = chr2;
            if (enc4 != 64) uarray[i+2] = chr3;
        }

        return uarray;
    }
}

function render(src, canvas) {
    if (src === undefined || src === '') {
        return
    }

    const arr = src.split(';');

    const x = arr.length === 3 ? arr[0] : 255
    const y = arr.length === 3 ? arr[1] : 255
    const tryi =  arr.length === 3 ? arr[2] : src

    if (!isNumeric(x) && !isNumeric(y)) {
        console.error('height and width must be numbers');
        return
    }
    canvas.width = x;
    canvas.height = y;

    if (canvas.getContext) {
        const ctx = canvas.getContext('2d');

        // background
        ctx.fillStyle = `rgb(255, 255, 255)`;
        ctx.fillRect(0, 0, x, y);

        const xScale = x / 255
        const yScale = y / 255

        // get the triangles
        const intArray = Base64Binary.decode(tryi)
        // format is x1, y1, x2, y2, x3, y3, r, g, b, a
        for (let i = 0; i < intArray.length; i += 10) {
            ctx.beginPath();
            ctx.fillStyle = `rgba(${intArray[6+i]}, ${intArray[7+i]}, ${intArray[8+i]}, ${intArray[9+i] / 255})`;
            ctx.moveTo(intArray[i] * xScale, intArray[1+i] * yScale);
            ctx.lineTo(intArray[2+i] * xScale, intArray[3+i] * yScale);
            ctx.lineTo(intArray[4+i] * xScale, intArray[5+i] * yScale);
            ctx.fill();
        }
    }
}

/**
 * render the tryi in the image element
 */
function renderTryi(imgElem) {
    imgElem.src = tryiUrl(imgElem.dataset.tryi)
}

function tryiUrl(tryi) {
    const canvas = document.createElement('canvas');
    render(tryi, canvas);
    return canvas.toDataURL();
}

function isNumeric(val) {
    return /^-?\d+$/.test(val);
}
